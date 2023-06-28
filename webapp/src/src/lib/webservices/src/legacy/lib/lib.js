/** ****************************************************************************
   Generic Helpers
***************************************************************************** */

export const pluck = (k, objs) =>
  objs.reduce((acc, obj) => [...acc, obj[k]], []);

export const uniq = (xs) => Array.from(new Set(xs));

export const isUpper = (c) => /[A-Z]/.test(c);

export const capitalize = (s) => `${s.charAt(0).toUpperCase()}${s.substr(1)}`;

export const camelToKebab = (s) =>
  Array.from(s).reduce(
    (acc, c, i) => acc + (isUpper(c) ? `${i ? "-" : ""}${c.toLowerCase()}` : c),
    ""
  );

export function kebabToCamel(s) {
  const splits = s.split("-");
  return splits[0] + splits.slice(1).map(capitalize).join("");
}

export const pluralize = (s, num = 2, suffix = "s") =>
  num > 1 ? s + suffix : s;

export function formatChoices(choices) {
  /* Format an array of string-type choices as a choice list string. */
  const lastIdx = choices.length - 1;
  switch (choices.length) {
    case 0:
      return "";
    case 1:
      return `${choices[0]}`;
    case 2:
      return `${choices[0]} or ${choices[1]}`;
    default:
      return `${choices.slice(0, lastIdx).join(", ")}, or ${choices[lastIdx]}`;
  }
}

export function populateTemplate(template, obj) {
  /* Return the result of interpolating the template string with
     the referenced obj properties.
     Example template: "/:account/collections/:collection"

     References with the suffix "|json" will be encoded as HTML-escaped JSON.
   */
  // Maybe need to support non-whole-path-segment replacements, but
  // let's just do that for now.
  const regex = /:[a-zA-Z_|]+/g;
  return template.replaceAll(regex, (match) => {
    const splits = match.slice(1).split("|");
    switch (splits.length) {
      case 1:
        return obj[splits[0]];
      case 2:
        if (splits[1] !== "json") {
          throw new Error(`Unsupported template filter: ${splits[1]}`);
        }
        return JSON.stringify(obj[splits[0]]).replace(/"/g, "&quot;");
      default:
        throw new Error(`Unparsable template reference: ${match}`);
    }
  });
}

/** ****************************************************************************
   Javascript Helpers
***************************************************************************** */

export const getBoundMethod = (ins, methodName) => ins[methodName].bind(ins);

export function ThrottledLIFO(minIntervalMs = 300) {
  /*
     Return a function that accepts a single (async) function argument
     whose invocations will be throttled based on minIntervalMs with only
     the last invocation being executed.
   */
  let handle;
  let _reject = () => undefined;
  // Return a promise that will be resolved in the case of execution or
  // rejected in the case of a throttle.
  return (func) => {
    _reject("throttled");
    clearTimeout(handle);
    return new Promise((resolve, reject) => {
      handle = setTimeout(async () => resolve(await func()), minIntervalMs);
      _reject = reject;
    });
  };
}
