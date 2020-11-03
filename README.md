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
| `-v<X>` | `-v3` | The verbosity of logging output. The higher the number more verbose messages are shown (range from 0-5)
| `-q` | - | Suppress all non essential output, usefull for scripts. Equivalent to `-v0`

## Commands

### `klo publish [options] <path>`

This command takes a repository path as argument, [builds it](#project-building) and ensure it's published as a Docker image and returns the image locator.

#### Publish Options

| Option | Default | Description |
| -- | -- | -- |
| `path` | `"."` | The project path to build. The acceptable paths are described in [Klo URIs](#klo-uris), the `klo` scheme is not allowed here.
| `-R --repo=url` | - | Docker base repository URL to wich the images will be pushed [$KLO_DOCKER_REPO]
| `--name=image` | - | Docker image name to replace project name
| `--tag=tag` | - | Docker image tag to replace project version
| `-L --local` | - | Load into images to [local docker daemon](#with-minikube).

#### Publish Examples

    klo publish

    klo publish path/to/repo

    klo publish github.com/user/repo

    klo publish --repo quay.io --name example --tag development github.com/user/repo

    klo publish --local github.com/user/repo

### `klo resolve [options]`

Takes a [YAML stream](#yaml-input) as input, resolve [Klo URIs](#klo-uris) and return the YAML stream.

#### Resolve Options

| Option | Default | Description |
| -- | -- | -- |
| `-f --filename=path` | - | The path to the [YAML set](#yaml-input) or `-` to read from standard input.
| `-R --repo=url` | - | Docker base repository URL to wich the images will be pushed [$KLO_DOCKER_REPO]
| `-L` | - | Load into images to [local docker daemon](#with-minikube).

#### Resolve Examples

    klo resolve -f manifest.yaml

    klo resolve -f- < manifest.yaml

    klo resolve -f manifests/

    klo resolve --repo quay.io -f manifests/

    klo resolve --local -f manifests/

### `klo apply [options]`

Auxiliary command to invoke [kubectl](https://github.com/kubernetes/kubectl) with the output of `klo resolve`.

#### Apply Options

The expected options are the same of `klo resolve` command plus the options expected by [kubectl apply](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands#apply) command.

#### Apply Examples

    klo apply -f manifests/ --serve-side=true --prune=true -l app=web

## Klo URIs

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

## With [minikube](https://github.com/kubernetes/minikube)

You can publish images with `klo` directly to `minikube` via the local Docker deamon.

An example of the required setup:

    # Use the minikube docker daemon.
    eval $(minikube -p minikube docker-env)
    
    # Make sure minikube is the current kubectl context.
    kubectl config use-context minikube
    
    # Deploy to minikube w/o registry.
    klo resolve -L -f config/ | kubectl apply -f-

With the `local` flag, `klo` register the image with the `klo.local` repository (can be overwriten with `--repo`) and forces the use of local Docker daemon to push the image to ensure the minikube's `docker-env` is used.

## Project Configuration

When a project is located, the configuration heuristic to determine the project settings are as follow:

- Check if the `${KLO_HOME}/config.edn` have an entry with the project name.
- Check if the current directory have a `.klo.edn` file with an entry with the project name.
- Check if the project directory contains a `.klo.edn` with the configuration.
- Check the project directory for know project builders:
  - If the project have a `project.clj` file, it's a `leiningen` project, it reads the configuration from its `:klo` [profile](https://github.com/technomancy/leiningen/blob/master/doc/PROFILES.md).

## Project Building

Currently only Leiningen uberjar builder is supported.

In the project configuration is possible to override the builder selection with the `:builder` attribute and the name of one of the supported builders. If the attribute is not present, the builder is guessed from the [configuration](#project-configuration).

### `:leiningen` builder

By default the Leiningen builder executes a `lein uberjar` in the project directory and extracts the generated uberjar file name from the output.

## Project Publishing

Publish is the process of taking a built project and publish it as a container image.

### `:uberjar` publisher

Takes the path of the build project's uberjar then creates a JVM-based image with `java -jar <uberjar>` as entrypoint, then publishes the image to a repository. The repository can be a remote registry or a [local one](#with-minikube).

## YAML Input

Where YAML input is expected, the available inputs are:

- YAML document: The path to a single file containing a single YAML document.
- YAML document set: The path to a single file containing multiple YAML documents [separated by](https://yaml.org/spec/1.0/#id2489959) `---`.
- YAML directory: The path to a directory containing YAML documents. The files into this directory are read recursivelly, depth first, the order is not guaranted.

### Bugs

Wut :bug:?

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
