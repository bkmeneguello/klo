# klo

`klo` is a tool for building and deploying Clojure applications to Kubernetes. Inspired by [Google Ko](https://github.com/google/ko)

## Installation

Download the JAR from <https://github.com/bkmeneguello/klo/releases/latest>.

## Usage

The package is distributed as a standalone executable JAR file. This command is mean to be aliased and henceforth will be refered only as `klo`:

    java -jar klo-0.1.0-standalone.jar [args]

#### Global Options

| Option | Default | Description |
| -- | -- | -- |
| -v\<X\> | -v3 | The verbosity of logging output. The higher the number more verbose messages are shown (range from 0-5)
| -q | - | Suppress all non essential output, usefull for scripts. Equivalent to `-v0`

## Commands

### `klo publish [options] <path>`

This command takes a repository path as argument, ensure it's published as a Docker image and returns the image locator.

#### Options

| Option | Default | Description |
| -- | -- | -- |
| path | "." | The project path to build. The acceptable paths are described in [Klo URIs](#uris), the `klo` scheme is not allowed here.
| -R --repo=url | - | Docker base repository URL to wich the images will be pushed [$KLO_DOCKER_REPO]
| --name=image | - | Docker image name to replace project name
| --tag=tag | - | Docker image tag to replace project version
| -L | - | Load into images to [local docker daemon](#minikube).

#### Examples

    klo publish

    klo publish path/to/repo

    klo publish github.com/user/repo

...

## <a name="uris"></a>Klo URIs

Klo supports mutiple URI schemes to define the project location, both local and remote.

Local URIs:

- `path/to/repo`
- `./path/to/repo`
- `../path/to/repo`
- `/path/to/repo`
- `C:\path\to\repo`
- `file:///path/to/repo`

Remote URIs:

- `github.com/user/repo`
- `https://github.com/user/repo.git`
- `ssh://git@github.com:user/repo.git`
- `https://example.com/archived/repo.zip` (also `http` schema and `bz2`, `gz`, `tar`, `tar.bz2`, `tar.gz`, `tar.xz`, `tgz`, `tbz`, `txz` archive formats)

## <a name="minikube"></a>With [minikube](https://github.com/kubernetes/minikube)

You can publish images with `klo` directly to `minikube` via the local Docker deamon.

An example of the required setup:

    # Use the minikube docker daemon.
    eval $(minikube -p minikube docker-env)
    
    # Make sure minikube is the current kubectl context.
    kubectl config use-context minikube
    
    # Deploy to minikube w/o registry.
    klo resolve -L -f config/ | kubectl apply -f-

With the `local` flag, `klo` register the image with the `klo.local` repository (can be overwriten with `--repo`) and forces the use of local Docker daemon to push the image to ensure the minikube's `docker-env` is used.

### Bugs

...

### Any Other Sections

### That You Think

### Might be Useful

## License

Copyright © 2020 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
<http://www.eclipse.org/legal/epl-2.0>.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at <https://www.gnu.org/software/classpath/license.html>.
