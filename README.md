# Unused-deps

Find unused dependencies in `deps.edn` or `project.clj`.

## Installation

### Babashka

With babashka you have the following options:

#### bbin

To install this tool with [bbin](https://github.com/babashka/bbin):

```
$ bbin install io.github.borkdude/unused-deps
```

Then run in your project:

```
$ unused-deps
{:unused-deps [[clj-kondo/clj-kondo {:mvn/version "2025.04.07"}]]}
```

#### Babashka tasks

Add this to your `bb.edn`:

```
:tasks {unused-deps {:task (prn (exec 'borkdude.unused-deps/unused-deps))}}
```

along with how you would normally add this library to `deps.edn`, so under `:deps` add:

```
:deps {io.borkdude/unused-deps {:git/sha "<latest-sha>"}
```

### Clojure

You can use the `borkdude.unused-deps/exec-fn` as an `:exec-fn` in `deps.edn`.

### Programmatic API

Require the namespace `borkdude.unused-deps` and use the `unused-deps` function.

## Options

Here are some options with examples which are hopefully self-explanatory. If
not, bug me on slack or make a Github issue.

```
--root-dir .
--deps-file deps.edn / project.clj (relative to root-dir)
--classpath (optional, normally derived from deps-file)
--source-paths src (default)
```

The source paths are checked for namespaces that are required. If there is a
`.jar` file that doesn't contain any of these namespaces it's considered unused.

## License

MIT, see [LICENSE](LICENSE).
