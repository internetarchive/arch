import { esbuildPlugin } from "@web/dev-server-esbuild";
import { playwrightLauncher } from "@web/test-runner-playwright";

export default {
  browsers: [
    playwrightLauncher({
      product: "chromium",
      launchOptions: {
        headless: true,
        devtools: true,
      },
    }),
  ],
  concurrency: 10,
  files: ["test/**/*.test.ts"],
  nodeResolve: true,
  playwright: true,
  plugins: [esbuildPlugin({ ts: true })],
  watch: true,
};
