---
{
    :title "Example Blog Post"
    :published-at #inst "2022-06-10T22:00:01-07:00"
}
---

# {% $title %}

MarkDoc is a superset of the [CommonMark Specification](https://commonmark.org).
This document is a playground for testing our implementation.

## Code Highlightng

```javascript
const name = "Luke Skywalker";

function sayHello(name) {
  console.log(`Hello ${name}!`);
}
```

```clojure
(def name "Luke Skywalker")

(defn say-hello [name]
  (println (str "Hello " name "!")))
```