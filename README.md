# IBMiProgTool
IBM i Programming Tool

Created by Vladimír Župka, 2017, 2025

vzupka@gmail.com


This project is not to be installed, it is ready to use as a Java application in the directory of the project. The application requires Java SE 8 or higher installed in PC.

User documentation can be found in the subdirectory "helpfiles" and it is also accessible under the menu Help in the running application.

This application supports creating, editing and compiling of programs, as well as finding errors from the compilation listing. Copying files between IBM i and PC in both directions with proper encoding is enabled.

When editing source files, hihglighting of blocks in diffrent languages (e.g. if - endif, dow - enddo, etc. in RPG) can be set on or off.

When compiling, you can select compile command (e.g. CRTBNDRPG, CRTRPGMOD), set the correct library list, and choose other options. Compile command can be changed by adding or removing parameters, or even be completely replaced by any batch CL command.

Application programs cooperate with classes in IBM Toolbox for Java (or JTOpen). Java classes require "host servers" running in IBM i and profile QUSER enabled.

The application has been created and tested in systems macOS and Windows. <b>Note:</b> Main class "MainWindow" contains the main() method that starts the application processing.

Processing starts with opening the file <b>IBMiProgTool.jar</b> (or double click).

Version 02.01:

- Added pattern matching for libraries, files and members.
- Expanding tree nodes runs in parallel.
- Rectangular selection of text in the editor was added with shift, Copy, Cut, Paste and Delete.

Version 02.02

- Split editor area.
- Find and replace text in separate window.
- Button "Last spooled file" added to Compile window.
- Different control elements redesigned in editor widnow.
- Bugs fixed.

Version 03.01

- Searching pattern in multiple files.
- Saving compiled file source type and compile command.
- Bugs fixed.

Version 03.02

- Enhancing stability - keeping or retrying connection to the server.
- Bug fixes.

Version 03.03

- Source user documentation in Pages added to "documents" directory.
- Compilation attributes saving and restoring corrected and redesigned.

Version 03.04

- Editor now enables color hihglighting SQL statements - in standalone script files or in programs (preprocessor EXEC SQL).

Version 03.05 April 2020

- Several functions were improved.
- Documentation was improved.
- Java Native Access updated to version 5.6.0

Version 04 February 2025

- Highlighting in the editor made faster (corrected big deficiency – the longer the text, the slower process).

Version 04.02 August 2025

- Added menu item "changeCCSID" in the menu at IFS directory and IFS file.
- Added device QPRINT and output queue QPRINT.
- In the "Compile" window:
  SQL scripts (source type SQL) can be performed.
  The compile command can be copied and changed, but also replaced by any other CMD command and performed.
  "Edit" button was removed.
  Functionality of the "Job log" button was enabled.
  More complete message list colored according the message type.
- In the "Change library list" window, libraries QGPL and QTEMP can be added to the user library list
  from the combo box.

Version 05.01 September 2025

- Added button "Servers" for maintaining list of server names/addresses and selecting a server to connect.
- Report a message if connection failes due to timeout in ping test.
- Corrected behavior of the caret in the text of a file at "display" option from the context menu.
- End of a spooled file data is preferably displayed in the window.
- Files of standard type (PDF, JPG, CSV, etc.) can be displayed by a context menu command.

Version 06.01 February 2026

- Change displaying .MBR suffix of source file members to their real types, e. g. .RPGLE, .CLLE, etc.
- Renaming of a source file and source file member allows changing only the bare name, without type suffix.
- Real type of a source file member is defined in creating the member using context menu command. Thereafter it can be changed only with CL command WRKMBRPDM.

- - - - - - - - - -

Tento projekt se neinstaluje, je okamžitě k použití jako Java aplikace v adresáři projektu. Aplikace vyžaduje Javu SE 8 instalovanou v PC.

Uživatelskou dokumentaci lze nalézt v podadresáři "helpfiles" a je také přístupná pod nabídkou Help v běžící aplikaci.

Tato aplikace podporuje vytváření, editaci a kompilaci programů, jakož i hledání chyb v protokolu o kompilaci. Je možné kopírovat soubory mezi IBM i a PC v obou směrech s odpovídajícím kódováním.

Při editaci zdrojových souborů lze zapnout nebo vypnout zvýraznění bloků v různých jazycích (např. if - endif, dow - enddo, atd. v RPG).

Při kompilaci lze vybrat kompilační příkaz (např. CRTBNDRPG, CRTRPGMOD), nastavit správný seznam knihoven a vybrat další volby. Kompilační příkaz je možné změnit doplněním nebo odebráním parametrů, anebo jej celý nahradit libovolným dávkovým CL příkazem.

Aplikační programy spolupracují s třídami v soustavě IBM Toolbox for Java (nebo JTOpen). Tyto třídy vyžadují, aby v IBM i běžely "host servery" a aby byl aktivován profil QUSER.

Aplikace byla vytvořena a testována v systémech macOS a Windows. <b>Poznámka:</b> Hlavní třída "MainWindow" obsahuje metodu main() zahajující aplikaci.

Výpočet se spouští otevřením souboru <b>IBMiProgTool.jar</b> nebo poklepáním.

Verze 02.01:

- Přidán výběr podle vzorku pro knihovny, soubory a členy.
- Rozevírání uzlů stromu běží paralelně.
- Přidán výběr obdélníkové oblasti textu v editoru s posuvem a operacemi Copy, Cut, Paste a Delete.

Verze 02.02

- Dělená oblast editoru.
- Hledání a náhrada textu v samostatném okně.
- Do okna Compile přidáno tlačítko "Last spooled file".
- Změněny a přemístěny ovládací prvky v okně editoru.
- Opraveny chyby.

Verze 03.01

- Hledání vzorku ve více souborech.
- Ukládání uživatelem zvolených atributů (zdrojového typu a kompilačního příkazu) pro kompilované soubory.
- Oprava chyb.

Verze 03.02

- Zlepšení stability - udržení nebo obnova spojení se serverem.
- Oprava chyb.

Verze 03.03

- Uživatelská dokumentace v Pages přidána do adresáře "documents".
- Ukládání a obnova atributů kompilace opraveno a přepracováno.

Verze 03.04

- Editor nyní umožňuje barevné zvýraznění SQL příkazů - v samostatných souborech skriptů nebo v programech (EXEC SQL předkompilátoru).

Verze 03.05 duben 2020

- Byly zlepšeny některé funkce.
- Byla zlepšena dokumentace.
- Java Native Access povýšen na verzi 5.6.0

Verze 04 únor 2025

- Zrychleno zvýrazňování v editoru (opraven velký nedostatek – čím delší text, tím pomalejší proces).

Verze 04.02 srpen 2025

- Přidána volba "changeCCSID" v nabídce u IFS adresáře a IFS souboru.
- Přidáno zařízení QPRINT a výstupní fronta QPRINT.
- V okně "Compile":
  Lze provádět i SQL skripty (zdrojový typ SQL).
  Kompilační příkaz je možné kopírovat a měnit, ale i nahradit jej jiným CMD příkazem a spustit.
  Odstraněno tlačítko "Edit".
  Zajištění funkčnosti tlačítka "Job log".
  Kompletnější výpis zpráv označených barevně podle typu.
- V okně "Change library list" lze přidat knihovny QGPL a QTEMP do uživatelského seznamu knihoven
  z rozevírací nabídky.

Verze 05.01 říjen 2025

- Přidáno tlačítko "Servers" pro údržbu seznamu adres a názvů serverů a výběr serveru k připojení.
- Hlášení zprávy, když připojování selže v důsledku prodlevy v testu ping.
- Úprava chování ukazatele textu při zobrazení souboru volboou "display" z kontextové nabídky.
- U tiskového souboru se v okně zobrazí konec dat.
- V PC lze pomocí kontextové nabídky zobrazit soubory standardních typů (PDF, JPG, CSV atd).

Verze 06.01 Únor 2026

- Změna zobrazení přípony .MBR u členů zdrojových souborů na jejich pravé typy, například .RPGLE, .CLLE, apod.
- Přejmenování zdrojového souboru a členu zdrojového souboru je omezeno na holé jméno, bez koncovky typu.
- Skutečný typ členu zdrojového souboru je určen při vytvoření členu použitím příkazu kontextové nabídky. Typ může pak být změněn jen CL příkazem WRKMBRPDM.
