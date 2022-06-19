(ns build
  (:require ["zx" :refer [glob fs]]
            ["@markdoc/markdoc$default" :as markdoc]
            ["path" :as path]
            [nbb.core :refer [slurp await]]
            [clojure.string :as str]
            [promesa.core :as p]))

(def dist-folder "dist")
(def template (fs.readFileSync "template.html" "utf8"))

(defn markdown-to-html [markdown]
  (-> markdown
      (markdoc/parse)
      (markdoc/transform)
      (markdoc/renderers.html)))

(defn make-templated-html [title content]
  (as-> title $
    (str/replace template "{{ TITLE }}" $)
    (str/replace $ "{{ CONTENT }}" content)))

(defn process-post-path [post-path]
  (p/let [post (slurp post-path)
          post-html (markdown-to-html post)
          templated-html (make-templated-html "TODO" post-html)
          slug (-> (path/dirname post-path)
                   (path/basename))]
    {:path post-path
     :slug slug
     :html templated-html}))

(defn build []
  (fs.emptyDir dist-folder)
  (p/let [posts (glob "posts/**/*.md")
          posts (js->clj posts)
          posts (p/all (map process-post-path posts))
          _ (p/all
             (map (fn [p] (let [post-path (:path p)
                                destfolder (path/join dist-folder (:slug p))]
                            (p/do
                              (fs.emptyDir destfolder)
                              (fs.copy (path/dirname post-path) destfolder)
                              (fs.remove (path/join destfolder "index.md"))
                              (fs.writeFile (path/join destfolder "index.html") (:html p))))) posts))]
    (prn posts)
    posts))

(build)

(comment
  (markdown-to-html "# Test")
  (await (build))
)