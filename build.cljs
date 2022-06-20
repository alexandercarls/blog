(ns build
  (:require ["zx" :refer [glob fs]]
            ["@markdoc/markdoc$default" :as markdoc :refer [Tag]]
            ["@sindresorhus/slugify$default" :as slugify]
            ["path" :as path]
            ["react$default" :as React]
            ["prism-react-renderer$default" :refer [Prism]]
            ["prism-react-renderer$default.default" :as Highlight]
            [reagent.dom.server :as srv]
            [reagent.core :as r]
            [nbb.core :refer [slurp await]]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [applied-science.js-interop :as j]
            [promesa.core :as p]))

(def dist-folder "dist")
(def template (fs.readFileSync "template.html" "utf8"))

(defn date->human [date]
  (.toLocaleDateString date "en-US" #js {:year "numeric" :month "long" :day "numeric"}))
(def md-to-human-date {:transform (fn [parameters] (date->human (j/get parameters 0)))})

(defn parse-fronmatter [ast]
  (when-let [frontmatter (j/get-in ast [:attributes :frontmatter])]
    (edn/read-string frontmatter)))

(defn add-prismjs-language [language]
  (set! (.-Prism js/global) Prism)
  ((js/require "prismjs/components/") language))

(add-prismjs-language "clojure")

(defn is-line-highlighted-fn [highlight]
  (let [lines (->> (str/split highlight #",")
                   (map #(js/parseInt % 10))
                   (set))]
    (fn [n] (some #(= n %) lines))))

(defn Fence [{:keys [language highlight content]}]
  [:> Highlight {"Prism" Prism :code content :theme nil :language language}
   (fn [params] (let [{tokens "tokens"
                       class "className"
                       style "style"
                       get-line-props "getLineProps"
                       get-token-props "getTokenProps"} (js->clj params)
                      line-highlighted? (is-line-highlighted-fn highlight)]
                  (r/as-element [:div.relative>pre.grid {:class [class] :style style}
                                 (map-indexed (fn [il line]
                                                [:div
                                                 (update-in (js->clj (get-line-props #js {:line line :key il}) :keywordize-keys true) [:class] conj (when (line-highlighted? (inc il)) "-mx-4 px-[0.7rem] border-l-4 border-yellow-400 bg-yellow-300/[0.25]"))
                                                 (map-indexed (fn [it token] [:span (js->clj (get-token-props (clj->js {:token token :key it})) :keywordize-keys true)])
                                                              line)]) tokens)
                                 [:div.absolute.top-0.right-0.rounded-b-lg.bg-gray-600.text-xs.text-slate-200.p-2.mr-2.font-mono
                                  {:style {:text-shadow "none" :line-height "0.2rem"}}
                                  language]])))])

(def node-fence {:render "Fence"
                 :attributes {:content {:type 'String}
                              :highlight {:type 'String}
                              :language {:type 'String}}})

(defn Heading [{:keys [id level children]}]
  (let [heading-tag (if (= level 1)
                      [:h1.not-prose.text-2xl.font-extrabold.tracking-tight.text-slate-900.md:text-3xl {:id id} children]
                      [(keyword (str "h" level)) {:id id :class "text-lg md:text-2xl"} children])]
    [:a.no-underline.relative {:href (str "#" id)} heading-tag]))

(def node-heading {:render "Heading"
                   :children ["inline"]
                   :attributes {:id {:type 'String}
                                :level {:type 'Number :required true :default 1}}
                   :transform (fn [node config] (let [attributes (.transformAttributes node config)
                                                      children (.transformChildren node config)
                                                      id (slugify (first children))]
                                                  (Tag.
                                                   "Heading"
                                                   (clj->js (into (js->clj attributes) {:id id}))
                                                   children)))})

(defn markdown-to-react-elements [markdown]
  (let [ast (markdoc/parse markdown)
        frontmatter (parse-fronmatter ast)
        rendertree (markdoc/transform ast (clj->js {:variables frontmatter
                                                    :functions {:toHumanDate md-to-human-date}
                                                    :nodes {:heading node-heading
                                                            :fence node-fence}}))
        react-elements (markdoc/renderers.react
                        rendertree
                        React
                        (clj->js {:components {"Heading" (r/reactify-component Heading)
                                               "Fence" (r/reactify-component Fence)}}))]
    [react-elements frontmatter]))

(defn make-templated-html [title content]
  (as-> title $
    (str/replace template "{{ TITLE }}" $)
    (str/replace $ "{{ CONTENT }}" content)))

(defn post-layout [date content]
  [:article.relative.pt-8.mt-6
   [:div.text-sm.leading-6
    [:dl
     [:dd.absolute.top-0.inset-x-0.text-slate-700
      [:time {:date-time (.toISOString date)} (date->human date)]]]]
   [:div.prose.prose-slate.max-w-none content]])

(defn process-post-path [post-path]
  (p/let [post (slurp post-path)
          [post-react-element frontmatter] (markdown-to-react-elements post)
          post-html (srv/render-to-static-markup (post-layout (:published-at frontmatter) post-react-element))
          templated-html (make-templated-html (:title frontmatter) post-html)
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
    posts))

(build)

(comment
  (markdown-to-react-elements "# Test")
  (await (build)))