module.exports = {
  root: true,
  parser: "@typescript-eslint/parser",
  parserOptions: {
    tsconfigRootDir: __dirname,
    project: ["./tsconfig.json"],
  },
  plugins: ["@typescript-eslint"],
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:@typescript-eslint/recommended-requiring-type-checking",
    "prettier",
    "plugin:wc/recommended",
    "plugin:lit/recommended",
    "plugin:lit-a11y/recommended",
  ],
  overrides: [
    {
      files: ["src/**/*.ts"],
      rules: {
        // binding elements in lit templates has perf implications
        // https://github.com/43081j/eslint-plugin-lit/blob/master/docs/rules/no-template-bind.md
        "@typescript-eslint/unbound-method": ["off"],
        // Many params are coming from python api are snake_case
        camelcase: "off",
      },
    },
  ],
  ignorePatterns: ["src/tests/**/*.ts", "src/**/*.test.ts"],
};
