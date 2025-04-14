# clojure to datastar expression transpiler

### REPL run
```
clojure -M:dev ;; (bring your own repl server)
```

Check out [`dev/user.clj`](./dev/user.clj) and [`dev/demo.clj`](./dev/demo.clj)


### Patching D*

The use of `let` requires a patched version of datastar, see `regex-iife-support.patch`. A patched copy of the [develop branch][dt] is in `src/datastar@patched.js` and is used by the demo.

[dt]: https://github.com/starfederation/datastar/tree/develop
