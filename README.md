# Unused-deps

Find unused dependencies in `deps.edn` or `project.clj`.

## Installation

### Babashka

With babashka you have the following options:

#### Babashka tasks

Add this to your `bb.edn`:

``` clojure
:deps {io.github.borkdude/unused-deps {:git/sha "<latest-sha>"}
:tasks {unused-deps {:task (prn (exec 'borkdude.unused-deps/unused-deps))
                     :exec-args {...} ;; other options here
                    }}
```

Then run `bb unused-deps`. For more information about babashka tasks, look
[here](https://book.babashka.org/#tasks).

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

## Limitations

- `:local/root` isn't supported yet (should be easy to add)

## False positives

- Some libraries, such as JDBC drivers, register themselves on the classpath and
  don't have any statically detectable code. These libraries might still be used but are reported as unused.

## License

MIT, see [LICENSE](LICENSE).
