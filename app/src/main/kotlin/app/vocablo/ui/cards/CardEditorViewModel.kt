package app.vocablo.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vocablo.cards.Card
import app.vocablo.cards.CardText
import app.vocablo.cards.CardType
import app.vocablo.cards.DuplicateResult
import app.vocablo.data.SettingsStore
import app.vocablo.data.VocabloRepository
import app.vocablo.dictionary.DictionaryEntry
import app.vocablo.dictionary.OfflineDictionary
import app.vocablo.dictionary.PartOfSpeech
import app.vocablo.dictionary.WordLookup
import app.vocablo.verbs.FormIndex
import app.vocablo.verbs.FormMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface OnlineLookup {
    data object Idle : OnlineLookup
    data object Loading : OnlineLookup
    data class Found(val entries: List<DictionaryEntry>) : OnlineLookup
    data object NotFound : OnlineLookup
    data object Off : OnlineLookup
}

data class EditorState(
    val spanish: String = "",
    val english: String = "",
    val example: String = "",
    val exampleEnglish: String = "",
    val regionNote: String = "",
    val type: CardType = CardType.WORD,
    val suggestions: List<DictionaryEntry> = emptyList(),
    /** Set when the typed word is a conjugated form, e.g. "tuve" is tener. */
    val formHint: FormMatch? = null,
    val online: OnlineLookup = OnlineLookup.Idle,
    /** Shown under the Spanish field while typing. */
    val liveDuplicate: DuplicateResult = DuplicateResult.None,
    /** Non-null while the duplicate dialog is open. */
    val duplicateDialog: DuplicateResult? = null,
    val deckNames: Map<Long, String> = emptyMap(),
    val editingId: Long? = null,
    val addedThisVisit: Int = 0,
    val message: String? = null,
    val done: Boolean = false,
) {
    val canSave: Boolean get() = spanish.isNotBlank() && english.isNotBlank()
}

/**
 * Add or edit one card. Typing shows offline dictionary suggestions instantly, flags
 * conjugated forms, and checks for duplicates across every deck.
 */
class CardEditorViewModel(
    private val repo: VocabloRepository,
    private val settings: SettingsStore,
    private val dictionary: () -> OfflineDictionary,
    private val online: WordLookup,
    private val forms: () -> FormIndex,
    private val deckId: Long,
    cardId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState(editingId = cardId))
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private var checkJob: Job? = null
    private var typingJob: Job? = null

    init {
        viewModelScope.launch {
            val names = repo.decks().associate { it.id to it.name }
            _state.update { it.copy(deckNames = names) }
            if (cardId != null) {
                val card = repo.card(cardId) ?: return@launch
                _state.update {
                    it.copy(
                        spanish = card.spanish, english = card.english, example = card.example,
                        exampleEnglish = card.exampleEnglish, regionNote = card.regionNote, type = card.type,
                    )
                }
            }
        }
    }

    fun onSpanish(text: String) {
        _state.update { it.copy(spanish = text, online = OnlineLookup.Idle) }
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            val (suggestions, hint) = withContext(Dispatchers.Default) {
                val words = dictionary().suggest(text)
                val typed = text.trim().lowercase()
                val match = forms().lookup(typed).firstOrNull()?.takeIf { m -> m.verb.infinitive != typed }
                words to match
            }
            _state.update { it.copy(suggestions = suggestions, formHint = hint) }
        }
        scheduleDuplicateCheck()
    }

    fun onEnglish(text: String) = _state.update { it.copy(english = text) }
    fun onExample(text: String) = _state.update { it.copy(example = text) }
    fun onExampleEnglish(text: String) = _state.update { it.copy(exampleEnglish = text) }
    fun onRegionNote(text: String) = _state.update { it.copy(regionNote = text) }
    fun onType(type: CardType) = _state.update { it.copy(type = type) }

    /** Appends a character from the accent row (á, ñ, ¿ ...). */
    fun appendChar(c: String) = onSpanish(_state.value.spanish + c)

    fun pick(entry: DictionaryEntry) {
        _state.update {
            it.copy(
                spanish = entry.spanish,
                english = entry.english,
                example = entry.example.ifEmpty { it.example },
                exampleEnglish = entry.exampleEnglish.ifEmpty { it.exampleEnglish },
                regionNote = entry.regionNote.ifEmpty { it.regionNote },
                type = when (entry.partOfSpeech) {
                    PartOfSpeech.VERB -> CardType.VERB
                    PartOfSpeech.PHRASE -> CardType.PHRASE
                    else -> CardType.WORD
                },
                suggestions = emptyList(),
                formHint = null,
                online = OnlineLookup.Idle,
            )
        }
        scheduleDuplicateCheck()
    }

    /** "tuve" was typed: switch the card to the infinitive instead. */
    fun useInfinitive(match: FormMatch) {
        _state.update {
            it.copy(
                spanish = match.verb.infinitive,
                english = it.english.ifBlank { match.verb.english },
                type = CardType.VERB,
                formHint = null,
                suggestions = emptyList(),
            )
        }
        scheduleDuplicateCheck()
    }

    fun dismissFormHint() = _state.update { it.copy(formHint = null) }

    fun lookUpOnline() {
        val word = _state.value.spanish
        if (word.isBlank()) return
        viewModelScope.launch {
            if (!settings.settings.first().onlineLookup) {
                _state.update { it.copy(online = OnlineLookup.Off) }
                return@launch
            }
            _state.update { it.copy(online = OnlineLookup.Loading) }
            val found = withContext(Dispatchers.IO) { runCatching { online.lookup(word) }.getOrDefault(emptyList()) }
            _state.update { it.copy(online = if (found.isEmpty()) OnlineLookup.NotFound else OnlineLookup.Found(found)) }
        }
    }

    fun save(force: Boolean = false) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            if (!force) {
                val dup = repo.checkDuplicate(s.spanish, s.english, ignoreCardId = s.editingId)
                if (dup != DuplicateResult.None) {
                    _state.update { it.copy(duplicateDialog = dup) }
                    return@launch
                }
            }
            val card = Card(
                id = s.editingId ?: 0,
                deckId = deckId,
                spanish = s.spanish,
                english = s.english,
                type = s.type,
                example = s.example,
                exampleEnglish = s.exampleEnglish,
                regionNote = s.regionNote,
            )
            if (s.editingId != null) {
                val old = repo.card(s.editingId)
                repo.updateCard(card.copy(deckId = old?.deckId ?: deckId, createdMillis = old?.createdMillis ?: 0))
                _state.update { it.copy(duplicateDialog = null, done = true) }
            } else {
                repo.addCard(card)
                clearForNext("Added “${s.spanish.trim()}”")
            }
        }
    }

    /** Duplicate dialog: put the new meanings on the card that already exists. */
    fun mergeIntoExisting(existing: Card) {
        val s = _state.value
        viewModelScope.launch {
            repo.mergeInto(existing.id, s.english, s.example, s.exampleEnglish)
            clearForNext("Updated “${existing.spanish}” with the new meaning")
        }
    }

    fun dismissDuplicate() = _state.update { it.copy(duplicateDialog = null) }

    fun delete() {
        val id = _state.value.editingId ?: return
        viewModelScope.launch {
            repo.deleteCard(id)
            _state.update { it.copy(done = true) }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    private fun clearForNext(message: String) {
        _state.update {
            EditorState(
                deckNames = it.deckNames,
                addedThisVisit = it.addedThisVisit + 1,
                message = message,
            )
        }
    }

    private fun scheduleDuplicateCheck() {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            delay(300)
            val s = _state.value
            val result = if (CardText.key(s.spanish).isEmpty()) {
                DuplicateResult.None
            } else {
                repo.checkDuplicate(s.spanish, s.english, ignoreCardId = s.editingId)
            }
            _state.update { it.copy(liveDuplicate = result) }
        }
    }
}
