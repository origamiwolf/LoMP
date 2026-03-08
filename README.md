# LoMP — Lots of Moving Parts

A dice roller and oracle aid for on-the-go tabletop roleplaying gamers.

LoMP is a lightweight Android companion app designed for solo or GM-less tabletop 
roleplaying. Roll dice, consult oracle tables, and keep your game moving without 
lugging around books or loose paper.

## Features

### Dice Roller
- Quick-roll buttons for common dice (d3 through d100)
- Custom dice expressions (e.g. `2d6+1d4+3`)
- Roll history showing your last 5 rolls
- Save frequently used expressions as named combinations
- Quick-roll buttons and saved combinations ordered by usage frequency

### Oracle
- Load oracle tables from a folder of JSON files on your device
- Navigate tables organised into folders and subfolders
- Tap any table to roll on it instantly
- Supports subtables, weighted ranges, and roll-twice entries
- Automatic verification of table structure with detailed error reporting

### Settings
- Export your saved dice combinations to a JSON file
- Import combinations from a previously exported file

## Installation

### From a Release APK
1. Download the latest APK from the [Releases](../../releases) page
2. On your Android device, enable **Install from unknown sources** in Settings
3. Open the downloaded APK and follow the prompts to install

### Build from Source
1. Clone the repository
```
   git clone https://github.com/origamiwolf/lomp.git
```
2. Open the project in Android Studio Panda or later
3. Connect your Android device via USB with debugging enabled
4. Run the app via **Run → Run 'app'**

**Requirements:**
- Android 8.0 (API 26) or later
- Android Studio Panda (AGP 9.0.1, Gradle 9.2.1, Kotlin 2.0.21)

## Oracle Tables

Oracle tables are JSON files you create and organise into folders on your device.
In the Oracle tab, tap **Select Folder** to point LoMP at your tables folder.
The folder structure on disk becomes the navigation structure in the app.

### JSON Format

Each table is a single `.json` file with the following structure:
```json
{
  "name": "Table Name",
  "totalSides": 6,
  "entries": [
    { "minRoll": 1, "maxRoll": 3, "result": { "type": "value", "text": "Result text" } },
    { "minRoll": 4, "maxRoll": 4, "result": { "type": "rollTwice" } },
    {
      "minRoll": 5,
      "maxRoll": 5,
      "result": {
        "type": "table",
        "name": "Subtable Name",
        "totalSides": 3,
        "entries": [
          { "minRoll": 1, "maxRoll": 1, "result": { "type": "value", "text": "Result A" } },
          { "minRoll": 2, "maxRoll": 2, "result": { "type": "value", "text": "Result B" } },
          { "minRoll": 3, "maxRoll": 3, "result": { "type": "value", "text": "Result C" } }
        ]
      }
    },
    { "minRoll": 6, "maxRoll": 6, "result": { "type": "value", "text": "Another result" } }
  ]
}
```

### Result Types

| Type | Description |
|------|-------------|
| `value` | A plain text result. Requires a `text` field. |
| `rollTwice` | Roll twice more on the same table and return both results. |
| `table` | Roll on an inline subtable. Requires `name`, `totalSides`, and `entries`. |

### Rules
- `minRoll` and `maxRoll` are inclusive. A range of `1–3` covers rolls 1, 2, and 3.
- Every number from 1 to `totalSides` must be covered by exactly one entry.
- Subtables follow the same rules as top-level tables.
- Subtable nesting beyond 2 levels will trigger a warning but will still load.
- Roll-twice chains are capped at 10 total results.

### Example Folder Structure
```
LoMP Tables/
├── yes_no.json
├── fantasy/
│   ├── encounters.json
│   ├── npc_traits.json
│   └── weather.json
└── sci-fi/
    ├── encounters.json
    └── planets.json
```

### Verification

When tables are loaded, LoMP checks each file for:
- Valid JSON structure
- Required fields present
- No gaps or overlaps in roll ranges
- Roll values within the valid range for `totalSides`
- Blank result text

Any errors or warnings are shown in a dismissible panel at the top of the Oracle tab.
Only tables that pass all checks are loaded and available to roll on.

## Dice Combinations

Saved dice combinations can be exported from **Settings → Export Combinations**.
This produces a `lomp_dice_combos.json` file you can save anywhere on your device
or transfer to another device.

To restore combinations, use **Settings → Import Combinations** and select your
exported file. This will replace all current saved combinations.

## Contributing

Contributions are welcome. Feel free to open issues for bugs or feature requests,
or fork the project and submit a pull request.

If you create oracle table sets you'd like to share, feel free to open an issue
linking to your tables.

