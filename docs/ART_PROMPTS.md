# Vocablo art prompts (Nano Banana Pro)
Version 1.2 | 2026-11-13

Every raster image in the app was generated with Gemini's **Nano Banana Pro** model
(`gemini-3-pro-image-preview`) at 2K. This file records the prompts so any image can be
regenerated or extended in the same style (NFR-8). Full-size sources (the raw generations and
the cut-out PNGs) are in the project's shared folder under `vocablo/art/`.

## How the set was made

1. **Master first.** Prompt 1 was run three times; the second result became *the Tuzo reference*.
2. **Reference attached to every later Tuzo prompt.** The reference image is sent with the prompt,
   together with the line in the "Character" block below. This is what keeps Tuzo identical
   across all poses and scenes. (Stepwise described its mascot in words only, and the results
   drifted; attaching the image fixed that here.)
3. **White backgrounds, then cut out.** Character art asks for a flat pure white background.
   The background is removed by flood-filling near-white pixels from the image edges, so white
   inside the character (eyes, teeth, cards) is kept, and the soft edge is recoloured to the
   outline brown so there is no white halo on dark themes.
4. **No text in images.** Small UI icons, words and the status-bar icon stay as vector art in
   the app, because image models draw blurry icons and misspell words.
5. **App sizes.** Poses 640 px, scenes 640 to 720 px, badges 384 px, game tiles 320 px, all WebP
   in `res/drawable-nodpi`. The launcher art is 432 px (108 dp at xxxhdpi) in `drawable-xxxhdpi`.

## Shared blocks

**Style** (end of every prompt):
> Style: polished 2D vector illustration for a mobile language-learning app, clean thick dark
> espresso brown (#2B1B12) outlines, flat colour with one soft cel-shade layer and a small
> highlight, no gradients, no 3D, no text, no letters, no watermark.

**Character** (start of every prompt that shows Tuzo, with the reference image attached):
> Use the attached image as the exact character reference: same pocket gopher Tuzo, same
> proportions, same caramel and cream fur, same big front teeth, same jade green bandana with
> marigold flowers, same outline.

**Full body** (poses): "Full body, centred, filling about 75% of the frame, on a flat pure white background."

Palette: Tuzo caramel #C9874B, cream #F6E2C2, jade #1E6B58, mint #BDEBDA, marigold #F5A623,
coral #E8604C, warm paper #FFF8EE.

## 1. Tuzo master (no reference)

> Character design of an original mascot named Tuzo: a small, chubby, baby pocket gopher from
> Mexico, cute and very expressive. Big round head, large shiny dark eyes with two white
> highlights each, round ears with pink insides, soft pink cheeks, a small dark brown nose, two
> big white front teeth showing in a wide happy smile, short whiskers. Caramel (#C9874B) fur with
> a cream (#F6E2C2) muzzle, belly and paws. Wears a small jade green (#1E6B58) bandana around the
> neck with a tiny marigold flower pattern. Short stubby legs, big feet, small round tail.
> Standing upright, three-quarter view, waving with one paw, the other paw holding a small white
> flash card, cheerful and welcoming. Full body, centred, filling about 80% of the frame, on a
> flat pure white background. Not a hamster, not a beaver, not a bear. + Style

Becomes `tuzo_idle` (mood IDLE).

## 2. Poses (Character + pose + Full body + Style)

| App drawable | Mood | Pose text |
| --- | --- | --- |
| `tuzo_ready` | READY | Standing ready and friendly, both paws holding a small stack of flash cards against the belly, small confident smile, looking at the viewer. |
| `tuzo_cheer` | CHEER | Jumping for joy with both arms up, eyes closed in happy arcs, huge open smile, feet off the ground, three small marigold sparkles around. |
| `tuzo_think` | THINK | One paw on chin, eyes looking up, small round mouth, three small grey thought dots above the head, holding a flash card in the other paw. |
| `tuzo_oops` | OOPS | Sheepish after a small mistake: one paw scratching the back of the head, wobbly friendly smile showing the front teeth, one small blue sweat drop. Kind, not sad. |
| `tuzo_sleep` | SLEEP | Curled up asleep on a small soft cushion, eyes closed, peaceful smile, a tiny "z" swirl, bandana still on. |
| `tuzo_proud` | WIN | Standing proudly holding up a shiny gold medal on a jade ribbon, chest puffed, big confident grin, two small sparkles. |

## 3. Launcher icon (Character + text + Style)

> App icon for a Spanish vocabulary app. Close-up of Tuzo's head and shoulders, big happy smile
> with the front teeth, peeking up out of a round dark burrow hole, one paw holding up a white
> flash card with a small marigold star on it. Bold, simple, readable at small sizes, centred
> with generous padding. The background is one solid flat jade green (#1E6B58) colour reaching
> every edge and corner of the square, with a soft mint circle behind Tuzo. No white areas, no
> fade, no vignette, no rounded corners, no border.

The first try faded to white at the bottom; the "reaching every edge" wording fixed it. The
adaptive icon places this art inside the 72 dp visible area on a matching jade (#2A7362) layer.

## 4. Scenes (Character + text + "Flat pure white background, centred, filling about 80% of the frame." + Style)

| App drawable | Scene text |
| --- | --- |
| `scene_session_complete` | Tuzo jumping in celebration with flash cards flying up like confetti, small marigold and coral confetti shapes and stars around. |
| `scene_all_done` | Tuzo relaxing in a colourful striped Mexican hammock tied between two small cacti, eyes closed, content smile, a tiny sun. |
| `scene_welcome_back` | Tuzo popping up out of a burrow hole and stretching both arms with a big yawn-smile, rubbing one eye, a small warm sunrise behind. |
| `scene_update_ready` | Tuzo carrying a small wrapped gift box with a marigold ribbon, excited smile. |
| `scene_empty_deck` | Tuzo peeking curiously into an empty open wooden card box, holding a pencil and one blank flash card, ready to write the first word. |
| `scene_notify` | Tuzo holding a small golden bell and ringing it gently, friendly wink, a few small musical sparkles. |
| `scene_onboarding` (9:16, background kept) | Tuzo with a small backpack walks along a winding path of round stepping stones toward a cheerful colourful Latin American hillside town with pastel houses, a small church tower, papel picado bunting without any letters, and green hills. Floating speech bubbles with no text hint at conversation. Warm paper sky, empty space in the top third. |

## 5. Badges (no reference)

> A round badge medallion for a mobile language-learning app achievement, on a flat pure white
> background. [INNER]. Marigold gold (#F5A623) rim, inner colour as stated, a single simple
> central symbol, bold and readable at small size, centred, filling 80% of the frame. + Style

| App drawable | Badge | [INNER] |
| --- | --- | --- |
| `badge_first_session` | First steps | Jade green inner, a single white flash card with a small gopher paw print on it. |
| `badge_streak_7` | On a roll | Coral inner, one friendly rounded flame. |
| `badge_streak_30` | Unstoppable | Deep plum (#5B2A6E) inner, a big rounded flame with a small crown of three marigold points above it. |
| `badge_words_100` | Cien palabras | Mint inner, an open book with three small blank flash cards fanning out of it; the cards are completely blank. |
| `badge_gold_verb` | Oro | Ocean blue (#1959C6) inner, one round gold medal with a star in its centre hanging from a short jade ribbon. Nothing else inside. |
| `badge_chest` | Treasure hunter | Jade green inner, a small open treasure chest glowing with marigold light. |

The first "100 words" try put letters on the cards and the first "gold verb" try drew a book;
both were regenerated with the stricter wording above.

## 6. Game tiles (no reference, background kept)

> Square game icon illustration for a mobile Spanish-learning game called [NAME]. [SCENE] Solid
> warm paper (#FFF3DF) background filling the whole square, simple bold composition centred
> with padding, readable at small size. No letters, no words, no numbers. + Style

| App drawable | [NAME] | [SCENE] |
| --- | --- | --- |
| `game_verb_rain` | Verb Rain | Rounded white word tiles with no writing falling like raindrops from a fluffy jade-green cloud, one tile glowing marigold as if tapped, small splash drops. |
| `game_tug_of_war` | Tug of War | A short thick rope crossing the middle of the tile with a coral flag knotted at its centre, both frayed ends visible, small motion lines. |
| `game_si_o_no` | Yes or No | Two big round buttons, a jade one with a white check and a coral one with a white cross, small sparkles. |
| `game_parejas` | Pairs | Four rounded cards in a 2 by 2 grid, two face up showing the same marigold sun, two face down with a jade pattern. |
| `game_cuatro` | Four Choices | Four rounded answer bubbles in jade, marigold, coral and ocean blue, the jade one glowing with a white check. |
| `game_sentences` | Sentence Builder | Three blank rounded word blocks snapping together like puzzle pieces, one lifting into place. |
| `game_tutor` | Tutor Chat | Two overlapping rounded speech bubbles, jade and marigold, three dots in one, a small sparkle. |
