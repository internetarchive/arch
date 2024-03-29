import{_ as e,b as t,s as o,y as s,a}from"./chunk-styles-4a7b21cd.js";import{t as c,A as l}from"./chunk-arch-alert-3c1ceea9.js";var r;let i=r=class extends o{constructor(){super(...arguments),this.collections=[],this.sourceCollectionIds=new Set}createRenderRoot(){return this}async connectedCallback(){super.connectedCallback(),await this.initCollections(),this.sourceCollectionIds=new Set(new URLSearchParams(window.location.search).getAll(r.urlCollectionsParamName))}get _formData(){const e={},t=Array.from(new FormData(this.form).entries());for(const[o,s]of t){const t=e[o];void 0===t?e[o]=s:Array.isArray(t)?e[o]=t.concat(s):e[o]=[t,s]}return e}render(){const{collections:e,sourceCollectionIds:t}=this,o=e.filter((e=>t.has(e.id)));return s`
      <div class="row">
        <div class="large-12 columns">
          <arch-alert
            .alertClass=${l.Primary}
            .message=${'Use this form to create a custom collection by filtering the contents of one or more existing source collections. You may use as many of the filtering options below as you desire and leave others blank. <a href="https://arch-webservices.zendesk.com/hc/en-us/articles/16107865758228" target="_blank">Learn about options and common choices here</a>. ARCH will email you when your custom collection is ready to use.'}
          >
          </arch-alert>

          <form>
            <label for="sources" class="required"> Source Collection(s) </label>
            <em id="sourceDesc">
              Select the collection(s) to use as the source for this custom
              collection.
            </em>
            <select
              name="sources"
              id="sources"
              aria-labelledby="source sourceDesc"
              required
              multiple
              size="8"
              style="resize: vertical;"
              ?disabled=${0===this.collections.length}
              @change=${this.sourceCollectionsChangeHandler}
            >
              ${0===this.collections.length?s`<option value="">Loading Collections...</option>`:s``}
              ${e.map((e=>s`
                  <option
                    value="${e.id}"
                    ?selected=${t.has(e.id)}
                  >
                    ${e.name}
                  </option>
                `))}
            </select>

            <label for="name" class="required"> Custom Collection Name </label>
            <em id="nameDesc">
              Give your custom collection a name to describe its contents.
            </em>
            <input
              type="text"
              name="name"
              id="name"
              aria-labelledby="name nameDesc"
              placeholder="${o.length>0?o[0].name:"Example Collection"} - My filters"
              required
            />

            <label for="surts"> SURT Prefix(es) </label>
            <em id="surtsDesc">
              Choose
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#document"
                target="_blank"
                >web documents</a
              >
              to include in your custom collection by their
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#surt"
                target="_blank"
                >SURT prefix/es</a
              >. Separate multiple SURTs with a <code>|</code> character and no
              space in-between.
            </em>
            <input
              type="text"
              name="surtPrefixesOR"
              id="surts"
              aria-labelledby="surts surtsDesc"
              placeholder="org,archive|gov,congress)/committees"
            />

            <label for="timestampFrom"> Crawl Date (start) </label>
            <em id="timestampFromDesc">
              Specify the earliest in a range of
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#timestamp"
                target="_blank"
                >timestamps</a
              >
              to include in your custom collection. Enter a full timestamp (in
              the <code>yyyyMMddHHmmSS</code> format), a prefix (ex.
              <code>yyyyMM</code>), or leave blank to include all web documents
              going back to the earliest collected.
            </em>
            <input
              type="text"
              name="timestampFrom"
              id="timestampFrom"
              aria-labelledby="timestampFrom timestampFromDesc"
              placeholder="19960115"
            />

            <label for="timestampTo"> Crawl Date (end) </label>
            <em id="timestampToDesc">
              Specify the latest in a range of
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#timestamp"
                target="_blank"
                >timestamps</a
              >
              to include in your custom collection. Enter a full timestamp (in
              the <code>yyyyMMddHHmmSS</code> format), a prefix (ex.
              <code>yyyyMM</code>), or leave blank to include all web documents
              up to the most recent collected.
            </em>
            <input
              type="text"
              name="timestampTo"
              id="timestampTo"
              aria-labelledby="timestampTo timestampToDesc"
              placeholder="19991231235959"
            />

            <label for="status"> HTTP Status </label>
            <em id="statusDesc">
              Choose web documents to include in your custom collection by their
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#status"
                target="_blank"
                >HTTP status code/s</a
              >. Separate multiple status codes with a <code>|</code> character
              and no space in-between.
            </em>
            <input
              type="text"
              name="statusPrefixesOR"
              id="status"
              aria-labelledby="status statusDesc"
              placeholder="200"
            />

            <label for="mime"> MIME Type </label>
            <em id="mimeDesc">
              Choose web documents to include in your custom collection by their
              file format/s, expressed as
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#mime"
                target="_blank"
                >MIME type/s</a
              >. Separate multiple MIME types with a | character and no space
              in-between.
            </em>
            <input
              type="text"
              name="mimesOR"
              id="mime"
              aria-labelledby="mime mimeDesc"
              placeholder="text/html|application/pdf"
            />

            <button type="submit" @click=${this.createSubCollection}>
              Create Custom Collection
            </button>
          </form>
        </div>
      </div>
    `}async initCollections(){this.collections=await(await fetch("/api/collections")).json()}setSourceCollectionIdsUrlParam(e){const{urlCollectionsParamName:t}=r,o=new URL(window.location.href);o.searchParams.delete(t),e.forEach((e=>o.searchParams.append(t,e))),history.replaceState(null,"",o.toString())}sourceCollectionsChangeHandler(e){const t=Array.from(e.target.selectedOptions).map((e=>e.value));this.sourceCollectionIds=new Set(t),this.setSourceCollectionIdsUrlParam(t)}async createSubCollection(e){e.preventDefault();const{form:t}=this;if(!t.checkValidity())return void t.reportValidity();const o=e.target;o.disabled=!0;const s=Object.fromEntries(Array.from(Object.entries(this._formData)).filter((([,e])=>""!==e)).map((([e,t])=>[e,"surtPrefixesOR"===e||"statusPrefixesOR"===e||"mimesOR"===e?t.split("|"):t]))),a=s.sources;let c;delete s.sources,Array.isArray(a)?(c="UNION-UDQ",s.input=a):c=a;(await fetch(`/api/runjob/UserDefinedQuery/${c}`,{method:"POST",body:JSON.stringify(s),headers:{"content-type":"application/json"}})).ok?window.location.href="/collections":(window.alert("Could not create Sub-Collection"),o.disabled=!1)}};i.urlCollectionsParamName="cid[]",e([c()],i.prototype,"collections",void 0),e([c()],i.prototype,"sourceCollectionIds",void 0),e([t("form")],i.prototype,"form",void 0),e([t("select#source")],i.prototype,"sourceSelect",void 0),i=r=e([a("arch-sub-collection-builder")],i);export{i as ArchSubCollectionBuilder};
//# sourceMappingURL=arch-sub-collection-builder-ff752dbf.js.map
