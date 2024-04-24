export const SAMPLE_JOB_ID_SUFFIX = "-SAMPLE";

export const HtmlStatusCodeRegex = /^[1-5]\d\d$/;

// SurtRegex: domain labels separated by commas optionally followed by ")" or ")/.*"
const ValidLabelChars = "[a-zA-Z0-9\\-]";
export const SurtPrefixRegex = new RegExp(
  `^${ValidLabelChars}+,${ValidLabelChars}+(${ValidLabelChars}+,?)*((\\))|(\\)/.*))?$`
);
