# Curse Pack Locator

This is an addon module using new forge systems to allow the automatic loading of any curse pack for 1.8~1.12.2.

## To use it:
* Install forge for Minecraft 1.8~1.12.2 which curse pack need. Grab that here: https://files.minecraftforge.net/
* Unzip the "bundle" package above, on top of your existing minecraft install folder (`.minecraft`)
* Modify the value of `inheritsFrom` in `.minecraft/versions/1.12.2-cursepacklocator/1.12.2-cursepacklocator.json` to the target version with forge.
* Launch the native vanilla launcher.
* Create a _new_ game _installation_ by clicking the `installations` tab and the `New` button there.
* Give the installation a name, select the `release 1.12.2-cursepacklocator` version and browse to a _new_ empty directory for your Game Directory.
* Extract the contents of the pack's zip file (downloaded from the curseforge website) into this new empty directory. It should contain a
`manifest.json` file and maybe other stuff as well.
* `Play` the new profile. The launch will pause for a moment while the curse pack downloader fetches all the mods from the curseforge website before launching into your game.

### Video

You can see my video on how to use it here on twitch: https://www.twitch.tv/videos/475677541
