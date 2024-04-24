import { assert, fixture, html, waitUntil } from "@open-wc/testing";
import { spy, stub } from "sinon";

import ArchAPI from "../src/lib/ArchAPI";

import "../src/archSubCollectionBuilder/index";

/*
 * Global stubs (WHERE DO THESE BELONG?)
 */

// Stub ArchAPI.collection.get to return a canned response.
stub(ArchAPI, "collections").get(() => ({
  get: async () => ({
    count: 2,
    results: [
      {
        id: "SPECIAL-test-collection-1",
        name: "ARCH Test Collection 1",
        public: false,
        size: "172.0 KB",
        sortSize: 176128,
        seeds: -1,
        lastCrawlDate: null,
        lastJobId: "WebGraphExtraction",
        lastJobSample: true,
        lastJobName: "Web graph (Sample)",
        lastJobTime: "2023-07-17 20:22:34",
      },
      {
        id: "SPECIAL-test-collection-2",
        name: "ARCH Test Collection 2",
        public: false,
        size: "172.0 KB",
        sortSize: 176128,
        seeds: -1,
        lastCrawlDate: null,
        lastJobId: "WebGraphExtraction",
        lastJobSample: true,
        lastJobName: "Web graph (Sample)",
        lastJobTime: "2023-07-17 20:22:34",
      },
    ],
  }),
}));

/*
 * Fixtures
 */

async function elInputsMapPair() {
  /*
   * Return a pair comprising a ArchSubCollectionBuilder element fixture
   * and an inputName -> inputElement map.
   */
  const el = await fixture(
    html`<arch-sub-collection-builder></arch-sub-collection-builder>`
  );
  const form = el.shadowRoot.querySelector("form");
  // Stub the el.form @query prop which seems to always return undefined
  // when under test.
  stub(el, "form").get(() => form);
  // Wait for collections to be initialized.
  el.update();
  const sourcesEl = form.querySelector("select[name=sources]");
  await waitUntil(() => sourcesEl.options.length === 2);
  const inputNames = [
    "sources",
    "name",
    "surtPrefixesOR",
    "timestampFrom",
    "timestampTo",
    "statusPrefixesOR",
    "mimesOR",
  ];
  return [
    el,
    Object.fromEntries(
      inputNames.map((n) => [n, form.querySelector(`[name="${n}"]`)])
    ),
  ];
}

async function elInputPair(inputName) {
  /*
   * Return a pair comprising a ArchSubCollectionBuilder element fixture
   * and the specified form input element.
   */
  const [el, inputsMap] = await elInputsMapPair();
  return [el, inputsMap[inputName]];
}

/*
 * Helpers
 */

function assertInputIsValid(el, inputEl, inputValue, decodedValue) {
  // Set the input to the specified value.
  inputEl.value = inputValue;
  // Decode the form data and use it to set form validity.
  const formData = el.formData;
  el.setFormInputValidity(formData);
  // Assert that the input state is valid and decoded value is as expected.
  assert.isTrue(inputEl.checkValidity());
  assert.deepEqual(formData[inputEl.name], decodedValue);
}

function assertInputIsInvalid(
  el,
  inputEl,
  inputValue,
  badValsStr,
  fullCustomValidation
) {
  // Set the input to the specified value.
  inputEl.value = inputValue;
  // Decode the form data and use it to set form validity.
  const formData = el.formData;
  el.setFormInputValidity(formData);
  // Assert that the input state is invalid and that the custom validity
  // message is as expected.
  assert.isFalse(inputEl.checkValidity());
  assert.equal(
    inputEl.validationMessage,
    fullCustomValidation ||
      `Please correct the invalid value(s): ${badValsStr ?? inputValue.trim()}`
  );
  assert.instanceOf(formData[inputEl.name], Error);
  // Assert that the next input event clears the custom validity state.
  inputEl.dispatchEvent(new Event("input", { bubbles: true }));
  assert.isTrue(inputEl.checkValidity());
  assert.empty(inputEl.validationMessage);
}

/*
 * Tests
 */

describe("ArchSubCollectionBuilder", () => {
  /*
   * SURT Prefix(es) form input tests
   */
  describe("surt prefixes form input", () => {
    // Test the empty string filtering that's applied to all optional inputs.
    it("filters out empty string values", async () => {
      const [el, inputEl] = await elInputPair("surtPrefixesOR");
      for (const [inputVal, decodedVal] of [
        ["", undefined],
        ["   ", undefined],
        ["|||||", undefined],
        ["  |  | | || |       |", undefined],
      ]) {
        assertInputIsValid(el, inputEl, inputVal, decodedVal);
      }
    });

    it("accepts and decodes a single valid value", async () => {
      const [el, inputEl] = await elInputPair("surtPrefixesOR");
      for (const [inputVal, decodedVal] of [
        ["org,archive", ["org,archive"]],
        ["org,arch-ive", ["org,arch-ive"]], // hyphen is valid label char
        ["org,archive)", ["org,archive)"]],
        ["org,archive)/", ["org,archive)/"]],
        ["org,archive)/a", ["org,archive)/a"]],
        ["org,archive)/a/b", ["org,archive)/a/b"]],
        ["org,archive)/a/b?c=1", ["org,archive)/a/b?c=1"]],
      ]) {
        assertInputIsValid(el, inputEl, inputVal, decodedVal);
      }
    });

    it("accepts and decodes multiple, delimited valid values", async () => {
      const [el, inputEl] = await elInputPair("surtPrefixesOR");
      for (const [inputVal, decodedVal] of [
        [
          "org,archive|org,archive-it)/a/b/?c=1",
          ["org,archive", "org,archive-it)/a/b/?c=1"],
        ],
        [
          "org,archive)|org,archive-it)/a/b",
          ["org,archive)", "org,archive-it)/a/b"],
        ],
        [
          "org,archive)/|org,archive-it)/a",
          ["org,archive)/", "org,archive-it)/a"],
        ],
        [
          "org,archive)/a|org,archive-it)/",
          ["org,archive)/a", "org,archive-it)/"],
        ],
        [
          "org,archive)/a/b|org,archive-it)",
          ["org,archive)/a/b", "org,archive-it)"],
        ],
        [
          "org,archive)/a/b?c=1|org,archive-it",
          ["org,archive)/a/b?c=1", "org,archive-it"],
        ],
      ]) {
        assertInputIsValid(el, inputEl, inputVal, decodedVal);
      }
    });

    it("rejects invalid single values", async () => {
      const [el, inputEl] = await elInputPair("surtPrefixesOR");
      for (const inputVal of [
        "org",
        "org,",
        "org,arch_ive", // underscore is invalid label char
        "org,archive/",
        "org,archive)a",
        "https://archive.org",
        "https://archive.org/a/b?c=1",
        "not even trying",
      ]) {
        assertInputIsInvalid(
          el,
          inputEl,
          inputVal,
          null,
          `Please correct the invalid SURT(s): ${inputVal}`
        );
      }
    });

    it("rejects invalid, multiple, delimited values", async () => {
      const [el, inputEl] = await elInputPair("surtPrefixesOR");
      for (const [inputVal, badValsStr] of [
        ["org,archive|org,", ["org,"]],
        ["org,archive|org,|org,archive)/|org,archive/", ["org,, org,archive/"]],
      ]) {
        assertInputIsInvalid(
          el,
          inputEl,
          inputVal,
          null,
          `Please correct the invalid SURT(s): ${badValsStr}`
        );
      }
    });
  });

  /*
   * Crawl Date (start) form input tests
   */
  describe("crawl date start form input", () => {
    it("decodes to ARCH timestamp string", async () => {
      const [el, inputEl] = await elInputPair("timestampFrom");
      for (const [inputVal, decodedVal] of [
        ["2023-08-08T12:00", "20230808120000"],
        ["2023-08-08T12:59", "20230808125900"],
      ]) {
        assertInputIsValid(el, inputEl, inputVal, decodedVal);
      }
    });
  });

  /*
   * Crawl Date (end) form input tests
   */
  describe("crawl date end form input", () => {
    it("decodes to ARCH timestamp string", async () => {
      const [el, inputEl] = await elInputPair("timestampTo");
      for (const [inputVal, decodedVal] of [
        ["2023-08-08T12:00", "20230808120000"],
        ["2023-08-08T12:59", "20230808125900"],
      ]) {
        assertInputIsValid(el, inputEl, inputVal, decodedVal);
      }
    });
  });

  /*
   * Combined Crawl Date (start/end) form input tests
   */
  describe("combined crawl date end/start form inputs", () => {
    it("accepts a valid range", async () => {
      const [el, inputsMap] = await elInputsMapPair();
      assertInputIsValid(
        el,
        inputsMap.timestampFrom,
        "2023-08-08T12:00",
        "20230808120000"
      );
      assertInputIsValid(
        el,
        inputsMap.timestampTo,
        "2023-08-08T12:01",
        "20230808120100"
      );
    });

    it("rejects a range where timestampFrom > timestampTo", async () => {
      const [el, inputsMap] = await elInputsMapPair();
      assertInputIsValid(
        el,
        inputsMap.timestampFrom,
        "2023-08-08T12:01",
        "20230808120100"
      );
      assertInputIsInvalid(
        el,
        inputsMap.timestampTo,
        "2023-08-08T12:00",
        null,
        "Crawl Date (end) must be later than Crawl Date (start)"
      );
    });

    it("rejects a range where timestampFrom == timestampTo", async () => {
      const [el, inputsMap] = await elInputsMapPair();
      assertInputIsValid(
        el,
        inputsMap.timestampFrom,
        "2023-08-08T12:01",
        "20230808120100"
      );
      assertInputIsInvalid(
        el,
        inputsMap.timestampTo,
        "2023-08-08T12:01",
        null,
        "Crawl Date (end) must be later than Crawl Date (start)"
      );
    });
  });

  /*
   * HTTP Status form input tests
   */
  describe("status code form input", () => {
    it("accepts and decodes a single valid value", async () => {
      const [el, inputEl] = await elInputPair("statusPrefixesOR");
      for (const [inputVal, decodedVal] of [
        ["100", ["100"]],
        ["200", ["200"]],
        ["302", ["302"]],
        ["400", ["400"]],
        ["404", ["404"]],
        ["503", ["503"]],
        ["599", ["599"]],
      ]) {
        assertInputIsValid(el, inputEl, inputVal, decodedVal);
      }
    });

    it("accepts and decodes multiple, valid, delimited values", async () => {
      const [el, inputEl] = await elInputPair("statusPrefixesOR");
      for (const [inputVal, decodedVal] of [
        ["100|200", ["100", "200"]],
        ["302|400", ["302", "400"]],
        ["404|503", ["404", "503"]],
        ["599|", ["599"]],
      ]) {
        assertInputIsValid(el, inputEl, inputVal, decodedVal);
      }
    });

    it("rejects invalid single values", async () => {
      const [el, inputEl] = await elInputPair("statusPrefixesOR");
      for (const inputVal of ["-200", "0", "99", "600", "not good"]) {
        assertInputIsInvalid(
          el,
          inputEl,
          inputVal,
          null,
          `Please correct the invalid status code(s): ${inputVal}`
        );
      }
    });

    it("rejects invalid, multiple, delimited values", async () => {
      const [el, inputEl] = await elInputPair("statusPrefixesOR");
      for (const [inputVal, badValsStr] of [
        ["200|99", "99"],
        ["not good|200|300", "not good"],
        ["not good|200|300|alsoBad|0||", "not good, alsoBad, 0"],
      ]) {
        assertInputIsInvalid(
          el,
          inputEl,
          inputVal,
          null,
          `Please correct the invalid status code(s): ${badValsStr}`
        );
      }
    });
  });

  /*
   * MIME Type form input tests
   */
  describe("mime type form input", () => {
    it("accepts and decodes a single valid value", async () => {
      const [el, inputEl] = await elInputPair("mimesOR");
      for (const [inputVal, decodedVal] of [
        ["text/html", ["text/html"]],
        ["application/json", ["application/json"]],
        ["audio/aac", ["audio/aac"]],
        ["font/collection", ["font/collection"]],
        ["image/bmp", ["image/bmp"]],
        ["model/step", ["model/step"]],
        ["video/mp4", ["video/mp4"]],
      ]) {
        assertInputIsValid(el, inputEl, inputVal, decodedVal);
      }
    });

    it("accepts and decodes multiple, valid, delimited values", async () => {
      const [el, inputEl] = await elInputPair("mimesOR");
      for (const [inputVal, decodedVal] of [
        ["text/html|application/json", ["text/html", "application/json"]],
        ["audio/aac|font/collection", ["audio/aac", "font/collection"]],
        ["image/bmp|model/step", ["image/bmp", "model/step"]],
      ]) {
        assertInputIsValid(el, inputEl, inputVal, decodedVal);
      }
    });

    it("rejects invalid single values", async () => {
      const [el, inputEl] = await elInputPair("mimesOR");
      for (const inputVal of [
        "not good",
        "textual/html",
        "text/htmlized",
        "applications/json",
        "applications/jsonified",
        "audiofile/aac",
        "audio/aacdefg",
        "fontificate/collection",
        "font/collectionize",
        "imagaine/bmp",
        "image/bmpbmpitup",
        "modal/step",
        "model/stepbystep",
        "videotape/mp4",
        "video/mp4eva",
      ]) {
        assertInputIsInvalid(
          el,
          inputEl,
          inputVal,
          null,
          `Please correct the invalid MIME(s): ${inputVal}`
        );
      }
    });

    it("rejects invalid, multiple, delimited values", async () => {
      const [el, inputEl] = await elInputPair("mimesOR");
      for (const [inputVal, badValsStr] of [
        ["text/html|application/json|not/good", "not/good"],
        [
          "audio/aac|applicaton/json|font/collection|text/*",
          "applicaton/json, text/*",
        ],
        ["html|image/bmp|model/step", "html"],
      ]) {
        assertInputIsInvalid(
          el,
          inputEl,
          inputVal,
          null,
          `Please correct the invalid MIME(s): ${badValsStr}`
        );
      }
    });
  });

  /*
   * Form submit payload tests
   */
  describe("form submission", () => {
    it("hits the UserDefinedQuery/{collectionName} endpoint for a single source", async () => {
      const [el, inputsMap] = await elInputsMapPair();
      // Select a single input collection.
      Array.from(inputsMap.sources.options)
        .filter((x) => x.value === "SPECIAL-test-collection-1")
        .map((x) => (x.selected = true));
      // Enter values for the remaining input fields.
      inputsMap.name.value = "ARCH Test Collection - HTML Only 3";
      inputsMap.surtPrefixesOR.value = "org,archive|org,archive-it";
      inputsMap.timestampFrom.value = "2023-08-15T12:00";
      inputsMap.timestampTo.value = "2023-08-16T12:00";
      inputsMap.statusPrefixesOR.value = "200|403";
      inputsMap.mimesOR.value = "text/html|text/css";
      // Do the submit.
      const doPostSpy = spy(el, "doPost");
      el.form.querySelector("button[type=submit]").click();
      // Check the payload.
      assert.isTrue(
        doPostSpy.calledWith("SPECIAL-test-collection-1", {
          name: "ARCH Test Collection - HTML Only 3",
          surtPrefixesOR: ["org,archive", "org,archive-it"],
          timestampFrom: "20230815120000",
          timestampTo: "20230816120000",
          statusPrefixesOR: ["200", "403"],
          mimesOR: ["text/html", "text/css"],
        })
      );
    });

    it("hits the UserDefinedQuery/UNION-UDQ endpoint for multiple sources", async () => {
      const [el, inputsMap] = await elInputsMapPair();
      // Select multiple input collections.
      Array.from(inputsMap.sources.options)
        .filter(
          (x) =>
            x.value === "SPECIAL-test-collection-1" ||
            x.value === "SPECIAL-test-collection-2"
        )
        .map((x) => (x.selected = true));
      // Enter values for the remaining input fields.
      inputsMap.name.value = "ARCH Test Collection - HTML Only 3";
      inputsMap.surtPrefixesOR.value = "org,archive|org,archive-it";
      inputsMap.timestampFrom.value = "2023-08-15T12:00";
      inputsMap.timestampTo.value = "2023-08-16T12:00";
      inputsMap.statusPrefixesOR.value = "200|403";
      inputsMap.mimesOR.value = "text/html|text/css";
      // Do the submit.
      const doPostSpy = spy(el, "doPost");
      el.form.querySelector("button[type=submit]").click();
      // Check the payload.
      assert.isTrue(
        doPostSpy.calledWith("UNION-UDQ", {
          name: "ARCH Test Collection - HTML Only 3",
          surtPrefixesOR: ["org,archive", "org,archive-it"],
          timestampFrom: "20230815120000",
          timestampTo: "20230816120000",
          statusPrefixesOR: ["200", "403"],
          mimesOR: ["text/html", "text/css"],
          input: ["SPECIAL-test-collection-1", "SPECIAL-test-collection-2"],
        })
      );
    });
  });
});
