import typescript from "@rollup/plugin-typescript";
import resolve from "@rollup/plugin-node-resolve";
import { terser } from "rollup-plugin-terser";
import styles from "rollup-plugin-styles";
import json from "@rollup/plugin-json";
import del from "rollup-plugin-delete";
import commonjs from "@rollup/plugin-commonjs";
import fs from "fs";

// `npm run build` -> `production` is true
// `npm run dev` -> `production` is false
const production = !process.env.ROLLUP_WATCH;

const TEMPLATES_PATHS = ["../WEB-INF/layouts", "../WEB-INF/views"];
const TEMPLATE_FILE_EXTENSION = ".ssp";
const CHUNK_FILENAME_PREFIX = "chunk-";
const ENTRY_FILENAMES_TEMPLATE = "[name]-[hash].js";
const ENTRY_FILENAMES_REGEX = new RegExp("(.+)-[a-zA-z0-9]+.js");
const STATIC_JS_PATH = "/js/dist";

function updateTemplates() {
  return {
    name: "update-templates",
    writeBundle(option, bundle) {
      // Get the base filenames of the generated, non-chunk javascript files.
      const generatedEntryFilenames = Object.keys(bundle).filter(
        (x) => x.endsWith(".js") && !x.startsWith(CHUNK_FILENAME_PREFIX)
      );

      // Generate {generatedFilename} -> [ {regex}, {replacement} ] map where {regex} is
      // the pattern that will match the base generated filename with any hash, and
      // {replacement} string that includes {generatedFilename}.
      const generatedEntryFilenamesRegexReplacementMap = new Map(
        generatedEntryFilenames.map((generatedEntryFilename) => {
          const baseEntryFilename = ENTRY_FILENAMES_REGEX.exec(
            generatedEntryFilename
          )[1];
          const regex = new RegExp(
            `${STATIC_JS_PATH}/${baseEntryFilename}-[a-z0-9]+.js`,
            "g"
          );
          const replacement = `${STATIC_JS_PATH}/${generatedEntryFilename}`;
          return [generatedEntryFilename, [regex, replacement]];
        })
      );
      // Get the template HTML file paths.
      const templatePaths = TEMPLATES_PATHS.flatMap((templatesPath) =>
        fs
          .readdirSync(templatesPath)
          .filter((x) => x.endsWith(TEMPLATE_FILE_EXTENSION))
          .map((x) => `${templatesPath}/${x}`)
      );
      // Iterate over each template to do the replacements.
      for (const path of templatePaths) {
        // Read the template contents.
        let templateContents = fs.readFileSync(path, "utf-8");
        // Do all the replacements.
        for (const generatedEntryFilename of generatedEntryFilenames) {
          templateContents = templateContents.replace(
            ...generatedEntryFilenamesRegexReplacementMap.get(
              generatedEntryFilename
            )
          );
        }
        // Write the template contents back.
        fs.writeFileSync(path, templateContents, "utf-8");
      }
    },
  };
}

export default {
  input: [
    "./src/archCollectionDetailsDatasetTable/src/arch-collection-details-dataset-table.ts",
    "./src/archCollectionsCard/src/arch-collections-card.ts",
    "./src/archCollectionsTable/src/arch-collections-table.ts",
    "./src/archDatasetExplorerTable/src/arch-dataset-explorer-table.ts",
    "./src/archDatasetMetadataForm/src/arch-dataset-metadata-form.ts",
    "./src/archDatasetPublishingCard/src/arch-dataset-publishing-card.ts",
    "./src/archGenerateDatasetForm/src/arch-generate-dataset-form.ts",
    "./src/archRecentDatasetsCard/src/arch-recent-datasets-card.ts",
    "./src/archSubCollectionBuilder/src/arch-sub-collection-builder.ts",
  ],
  output: {
    dir: "../js/dist",
    format: "es",
    sourcemap: true,
    chunkFileNames: `${CHUNK_FILENAME_PREFIX}[name]-[hash].js`,
    entryFileNames: "[name]-[hash].js",
  },
  plugins: [
    typescript({ tsconfig: "./tsconfig.json" }),
    commonjs(),
    styles({
      mode: "extract",
    }),
    resolve({
      exportConditions: ["browser", production ? "production" : "development"],
    }),
    production && terser(),
    json(),
    /** Update template asset file references */
    updateTemplates(),
    /** Clean the output directory prior to each bundle */
    del({ targets: ["../js/dist/*.js", "../js/dist/*.map"], force: true }),
  ],
};
