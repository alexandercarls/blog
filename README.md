# Blog

## Setup the development environment

<details>
  <summary><b>Dependencies</b></summary>

- Install [Node.js](https://nodejs.org/en/)
- Install [nbb](https://github.com/babashka/nbb)

</details>

<details>
    <summary><b>Run</b></summary>

1.  Create a local configuration file from the example.
    ```shell
    nbb build.cljs
    ```

2. Instal dependencies
    ```shell
    npm i
    ```

3. Start the HTTP Server
    ```shell
    npx http-server ./dist
    ```

4. Start Tailwind CLI in watch mode
    ```bash
    npx tailwindcss -i ./base.css -o ./dist/output.css --watch
    ```

6. Open `http://localhost:8080/`

</details>

## Publishing

1. `nbb build.cljs`
2. `npx tailwindcss -i ./base.css -o ./dist/output.css --minify`
3. Deploy `dist` folder

---

https://www.alexandercarls.de