# Tuxcraft-Installer

*Copyright © 2020 MaVdbussche/Barasingha and Morgane Austreelis*

Tuxcraft installer is a [MultiMC](https://multimc.org/) instance installer
primarily designed for the semi-private **Tuxcraft modpack**.

This installer grabs a zipped MultiMC TuxCraft instance, installs it,
conserving local config of older versions already installed.

Be aware that this installer has not be tested with any other modpack or
launcher than MultiMC.


## Installation

If you don't have access to a compiled jar of this code, a `pom.xml` is
provided to build with maven. Run Maven in the root directory, the JAR should
be available in `target` folder.


## Usage

Run the JAR application from any directory. You will be asked the path to the
zipped MultiMC instance to install and the `instances` folder of your MultiMC
installation.

Once this is done, the installer detects any previously installed TuxCraft
instances and copies the newest in the new instance folder. The updated
instance files are then copied, replacing the older ones. The local config
files are kept untouched.

If no TuxCraft instance was installed, the new instance is installed and
provided with default config files.


## Contributing

Pull Requests are welcome. Favor sequence of small commits focusing on one
change, with descriptive message. You can request features or report bugs
using Github's issues.


## License

The content of this repository is licensed under the [GPLv3 license](https://www.gnu.org/licenses/gpl-3.0.html).

