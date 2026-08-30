# Paper Filtered Hoppers

Een plugin voor [Paper](https://papermc.io) (Minecraft **1.21.x**) waarmee je van **elke hopper** een slimme filterhopper maakt. Je stelt per hopper in welke items hij doorlaat of juist tegenhoudt, zodat je zelf sorteerinstallaties bouwt zonder redstone-gedoe.

Deze plugin richt zich 100% op de **functionaliteit**; voor meer technische details kun je terecht in de code of de config.

---

## In het kort

- **Elke hopper kan een filterhopper zijn.** Een hopper waarop je niets instelt gedraagt zich gewoon zoals vanille.
- **Geen tools, geen extra items.** Je bedient alles via shift-rechtsklikken.
- **Twee modi tegelijk per hopper:** een *toestaan*-lijst (groen) én een *blokkeren*-lijst (zwart). De hopper onthoudt beide tegelijk en kan ermee rekenen.
- **Rode rand** om filterhoppers, zodat je in één oogopslag ziet welke hoppers actief zijn.

---

## Hoe het werkt

### 1. Het filtermenu openen

Loop naar een hopper en **shift-rechtsklik** (sluipen + rechtsklikken). Het filtermenu opent zich. Normaal rechtsklikken blijft gewoon het hopperscherm openen.

### 2. Het menu lezen

Het menu heeft **9 slots**:

| Slot | Betekenis |
|------|-----------|
| **Slot 1** | De modus-indicator. **Groene emerald** = *toestaan* (groene modus). **Zwarte blackstone** = *blokkeren* (zwarte modus). |
| **Slot 2 t/m 9** | De fileritems van de momenteel getoonde modus (maximaal 8 items per modus). |

De naam en omschrijving van de indicator vertellen je altijd welke modus je aan het bewerken bent.

### 3. Filteritems toevoegen

- Zet het menu op de gewenste modus (zie punt 4).
- **Klik het item dat je wilt filteren vanuit je eigen inventaris in een leeg slot**.
- Sluit het scherm om op te slaan. De hopper weet het meteen.

### 4. Wisselen tussen de modi

Klik op de **emerald/blackstone** in slot 1. De hopper slaat de huidige modus op, wisselt naar de andere, en toont de items van die andere modus. Beide lijsten blijven dus gewoon bewaard: je kunt de groene en de zwarte modus tegelijk vullen.

### 5. Filter (gedeeltelijk) wissen

Open het menu, haal de items uit de slots en sluit het scherm. Een lege lijst is hetzelfde als "geen filter". Wil je beide modi helemaal leeg hebben, wis dan allebei.

---

## De filterlogica

De hopper gebruikt **beide lijsten tegelijk** om te beslissen of een item binnen mag:

1. **Zwarte lijst wint altijd.** Komt het item overeen met een item in de *blokkeren*-modus? Dan wordt het **tegengehouden**, ongeacht de groene lijst.
2. **Groene lijst is een whitelist.** Staat het item in de groene modus en niet in de zwarte? Dan wordt het **doorgelaten**.
3. **Groene lijst leeg** betekent: alles is welkom, behalve de geblokkeerde items.

| Groene lijst | Zwarte lijst | Gedrag |
|--------------|--------------|--------|
| Leeg | Leeg | Vanille: alles wordt doorgelaten |
| Gevuld | Leeg | Alleen de genoemde items worden doorgelaten (sorteer-filter) |
| Leeg | Gevuld | Alles wordt doorgelaten behalve de genoemde items (blokkade) |
| Gevuld | Gevuld | Alleen groene items die niet zwart zijn, worden doorgelaten |

De filter werkt op **elke manier waarop items binnenkomen**:
- grond-items die een hopper oppakt,
- items die een hopper uit een kist of andere container erboven zuigt,
- items die een andere hopper doorgeeft.

Een filterhopper **duwt** vervolgens alles wat erin zit gewoon verder (bijvoorbeeld naar een kist eronder), want alles wat erin ligt mag er per definitie zijn.

### Waarom beide lijsten nuttig zijn

- **Sorteerlijnen:** zet op elke filterhopper in de rij zijn eigen itemtype in de groene modus. Ieder itemttype valt dan precies bij zijn eigen hopper in de kist eronder, terwijl de rest doorloopt.
- **Blokkades:** wil je dat een lopende band alles doorlaat behalve één itemtype? Zet dat type in de zwarte modus en de groene modus blijft leeg.
- **Nauwkeurig combineren:** wil je "alle diamanten doorlaten, behalve diamant-zwaarden"? Groen = diamant, zwart = diamantzwaard, en klaar.

### Item-vergelijking

Standaard vergelijkt de plugin op **itemtype**: een filter met *ijzeren staaf* loot alle ijzeren staven door, ongeacht hun naam of enchantments.

Dit kun je in de config wijzigen naar **exacte vergelijking**: dan telt ook naam, lore en enchantments mee. Handig wanneer je bijvoorbeeld alleen een specifiek, benoemd zwaard wilt scheiden.

---

## De rode rand

Zodra een hopper een **actief filter** heeft (groen of zwart gevuld), krijgt hij een **rode gloeirand** van deeltjes langs de randen van het blok. Zo zie je in één oogopslag welke hoppers iets bijzonders doen.

- De rand verschijnt en verdwijnt vanzelf als je een filter opslaat of wist.
- Deeltjes zijn alleen te zien wanneer je **binnen ~32 blokken** staat; ver weg of in ongeladen gebieden kost het niets.
- Inclusief optimalisaties zodat het vrijwel geen serverprestatie kost.
- Uitschakelbaar via de config (`visual-enabled`).

---

## Configuratie

Alle instellingen staan in `config.yml` in de plug-inmap. Na een wijziging herlaad je met:

```
/hopperfilter reload
```

| Instelling | Standaard | Betekenis |
|------------|-----------|-----------|
| `gui-title` | `&0Hopper Filter` | Titel van het filtermenu (kleurcodes met `&` zijn toegestaan). |
| `match-by-type` | `true` | `true` = filteren op itemtype; `false` = exacte itemvergelijking (naam/lore/enchantments). |
| `visual-enabled` | `true` | Toont de rode rand om filterhoppers (`true`/`false`). |

---

## Installeren

1. Gebruik een **Paper**-server van **1.21.x** (de plugin werkt niet op Spigot).
2. Zorg dat er **Java 21** geïnstalleerd is.
3. Kopieer `PaperFilteredHoppers.jar` naar de map `plugins/` van je server.
4. Herstart de server (of gebruik een plugin-manager zoals PlugMan).
5. Je bent klaar: shift-rechtsklik een hopper en richt je filter in.

---

## Commando's en permissies

| Commando | Beschrijving | Permissie |
|----------|--------------|-----------|
| `/hopperfilter reload` | Herlaadt de config zonder server-herstart. | `hopperfilter.reload` (standaard alleen operators) |

Alleen de config-herlaad-actie heeft een permissie nodig; **alle spelers** kunnen gewoon shift-rechtsklikken om filters te beheren.

---

## Bekende grenzen

- Eén hopper kan maximaal **8 items per modus** aan (dus 8 groene + 8 zwarte).
- De filter is gekoppeld aan het **blok**, niet aan een speler: iedereen met toegang tot de hopper mag er filters op zetten.
- Verwijderde filterhoppers verliezen hun filter (het blok staat er niet meer).

---

## Versie

**0.5.0** — eerste publieke versie.

Wil je iets toevoegen of vind je een bug? Open een issue in dit repository.