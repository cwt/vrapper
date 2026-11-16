![vrapper](https://github.com/vrapper/vrapper/raw/master/releng/net.sourceforge.vrapper.releng.website/img/vrapper_logo.png)

Vim-like editing in Eclipse  [![Join the chat at https://gitter.im/vrapper/vrapper](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vrapper/vrapper?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
---------------------------

Vrapper is an Eclipse plugin which acts as a wrapper for Eclipse text editors to provide a Vim-like input scheme for moving around and editing text.

> **Fork Notice:** This repository is a fork of [vrapper/vrapper](https://github.com/vrapper/vrapper) created to modernize the project and target **JDK 25**.
>
> Notable updates in this fork:
> - Upgraded Eclipse Tycho (`tycho.version` and `tycho-pomless`) to `5.0.4` and configured `tycho-compiler-plugin` with `<useJDK>SYSTEM</useJDK>` for building with JDK 25.
> - Moved the PyDev integration (`net.sourceforge.vrapper.eclipse.pydev` and `net.sourceforge.vrapper.eclipse.pydev.feature`) into [`optional/`](optional/), excluding it from the default Maven build and update site.

Building with Maven
-------------------

### Prerequisites

- **JDK 25** installed and active in `JAVA_HOME` / `PATH` (`pom.xml` uses `<useJDK>SYSTEM</useJDK>`)
- **Apache Maven** (3.9+)
- Network access on the first build to download Eclipse and target platform dependencies defined in `releng/net.sourceforge.vrapper.releng.target/net.sourceforge.vrapper.releng.target.target`

### Build and Package

Run the full Maven/Tycho build (compiles plugins, runs tests, and generates the Eclipse p2 update site):

```bash
mvn clean verify
```

For non-interactive/CI builds:

```bash
mvn -B verify --file pom.xml
```

After a successful build, the p2 update site outputs are created under `releng/net.sourceforge.vrapper.releng.update-site/target/`:

- **Unpacked p2 repository:** `releng/net.sourceforge.vrapper.releng.update-site/target/repository/`
- **Zipped p2 archive:** `releng/net.sourceforge.vrapper.releng.update-site/target/net.sourceforge.vrapper.releng.update-site-0.75.0-SNAPSHOT.zip`

Installing into Eclipse
-----------------------

You can install the locally built plugin directly into Eclipse from the generated p2 update site:

1. Build the project with `mvn clean verify`.
2. In Eclipse, open **Help → Install New Software...**
3. Click **Add...** to add a local update site, then choose **one** of:
   - **Local...** and select `releng/net.sourceforge.vrapper.releng.update-site/target/repository`
   - **Archive...** and select `releng/net.sourceforge.vrapper.releng.update-site/target/net.sourceforge.vrapper.releng.update-site-0.75.0-SNAPSHOT.zip`
4. Enter a name (for example, `Vrapper Local`) and click **Add**.
5. Select the features you want to install from the categories:
   - **Vrapper** (core Vrapper feature and split-editor plugin)
   - **Optional Vimscript Plugins**
   - **Optional Programming Language Plugins** (CDT, JDT)
6. Click **Next >**, review the installation details, accept the license, and click **Finish**.
7. Confirm any trust prompt for locally built/unsigned artifacts and restart Eclipse when prompted.

Optional: Exporting and Installing PyDev Support via Eclipse PDE (`dropins`)
------------------------------------------------------------------------------

Because the PyDev integration is excluded from the default Maven build and update site, you can export it manually from Eclipse PDE as a JAR and install it via Eclipse's `dropins` folder.

### Prerequisites in Eclipse

- **Eclipse PDE** (Plug-in Development Environment, included in *Eclipse IDE for RCP and RAP Developers* or installable via *Install New Software...*).
- Both **Vrapper** (`net.sourceforge.vrapper.eclipse` `0.75.0+`) and **PyDev** (`org.python.pydev` `2.6.0+`) available to PDE—either installed in your running Eclipse IDE, or resolved via workspace projects (`plugins/net.sourceforge.vrapper.core` and `plugins/net.sourceforge.vrapper.eclipse`) / the target platform (`releng/net.sourceforge.vrapper.releng.target/net.sourceforge.vrapper.releng.target.target`).

### 1. Import and Export from Eclipse PDE

1. In Eclipse, go to **File → Import... → General → Existing Projects into Workspace**.
2. Browse to `optional/net.sourceforge.vrapper.eclipse.pydev` (and `optional/net.sourceforge.vrapper.eclipse.pydev.feature` if exporting the feature) and click **Finish**.
3. Export as a JAR using **one** of the following options:
   - **Plug-in / Fragment JAR only (simplest):**
     1. Right-click `net.sourceforge.vrapper.eclipse.pydev` → **Export... → Plug-in Development → Deployable plug-ins and fragments** → **Next >**.
     2. Ensure `net.sourceforge.vrapper.eclipse.pydev` is checked.
     3. Select **Destination → Directory** (e.g., a temporary folder like `/tmp/vrapper-pydev`).
     4. On the **Options** tab, keep **Package plug-ins as individual JAR archives** checked and click **Finish**.
     5. This creates `<dest>/plugins/net.sourceforge.vrapper.eclipse.pydev_0.75.0.<qualifier>.jar`.
   - **Feature + Plug-in JARs:**
     1. Right-click `net.sourceforge.vrapper.eclipse.pydev.feature` → **Export... → Plug-in Development → Deployable features** → **Next >**.
     2. Ensure `net.sourceforge.vrapper.eclipse.pydev.feature` is checked, choose **Destination → Directory**, and click **Finish**.
     3. This creates `<dest>/features/` and `<dest>/plugins/` containing the feature and fragment JARs.

### 2. Install via Eclipse `dropins`

1. Locate your Eclipse `dropins/` directory inside your Eclipse installation (e.g., `<eclipse-dir>/dropins/`, or `Eclipse.app/Contents/Eclipse/dropins/` on macOS).
2. Ensure **Vrapper** and **PyDev** are already installed in that Eclipse instance (required because `net.sourceforge.vrapper.eclipse.pydev` is a fragment of `net.sourceforge.vrapper.eclipse`).
3. Copy the exported artifact(s) into `dropins/`:
   - For the **fragment JAR**: copy `net.sourceforge.vrapper.eclipse.pydev_*.jar` into `<eclipse-dir>/dropins/` (or `<eclipse-dir>/dropins/vrapper-pydev/plugins/`).
   - For the **feature export**: copy both the exported `features/` and `plugins/` directories into `<eclipse-dir>/dropins/vrapper-pydev/` (so the layout is `dropins/vrapper-pydev/features/*.jar` and `dropins/vrapper-pydev/plugins/*.jar`).
4. Restart Eclipse (run `eclipse -clean` once if Eclipse does not pick up the new drop-in on startup).
5. Confirm it loaded under **Help → About Eclipse IDE → Installation Details → Plug-ins** (`net.sourceforge.vrapper.eclipse.pydev`).

Upstream Links
--------------

**Upstream Stable Update Site:** http://vrapper.sourceforge.net/update-site/stable

**Upstream Unstable Update Site:** http://vrapper.sourceforge.net/update-site/unstable

License: [GPL v3 or any later version](LICENSE.md)

Home Page: http://vrapper.sourceforge.net/home/

Documentation: http://vrapper.sourceforge.net/documentation/

Developer Wiki: https://github.com/vrapper/vrapper/wiki
