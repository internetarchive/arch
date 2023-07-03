import{_ as t,e,a as o,s as a,y as s,i,b as r}from"./chunk-styles-4a7b21cd.js";import{t as n,A as l}from"./chunk-arch-alert-3c1ceea9.js";import{e as c}from"./chunk-query-all-2f0be97e.js";var d,h;!function(t){t.ArsLgaGeneration="ArsLgaGeneration",t.ArsWaneGeneration="ArsWaneGeneration",t.ArsWatGeneration="ArsWatGeneration",t.AudioInformationExtraction="AudioInformationExtraction",t.DomainFrequencyExtraction="DomainFrequencyExtraction",t.DomainGraphExtraction="DomainGraphExtraction",t.ImageGraphExtraction="ImageGraphExtraction",t.ImageInformationExtraction="ImageInformationExtraction",t.PdfInformationExtraction="PdfInformationExtraction",t.PresentationProgramInformationExtraction="PresentationProgramInformationExtraction",t.SpreadsheetInformationExtraction="SpreadsheetInformationExtraction",t.TextFilesInformationExtraction="TextFilesInformationExtraction",t.VideoInformationExtraction="VideoInformationExtraction",t.WebGraphExtraction="WebGraphExtraction",t.WebPagesExtraction="WebPagesExtraction",t.WordProcessorInformationExtraction="WordProcessorInformationExtraction"}(d||(d={})),function(t){t.NotStarted="Not started",t.Queued="Queued",t.Running="Running",t.Finished="Finished",t.Failed="Failed"}(h||(h={}));const u="-SAMPLE";var p;!function(t){t.Generate="generate",t.View="view",t.Status="status"}(p||(p={}));let b=class extends a{createRenderRoot(){return this}jobStateToButtonProps(t){if(!t)return[this.collectionId?"Loading...":"n/a",p.Status,"job-statebutton"];const e=t.sample>0?"Sample ":"";return t.started?t.finished?[`View ${e}Dataset`,p.View,"job-resultsbutton"]:[t.state,p.Status,"job-statebutton"]:[`Generate ${e}Dataset`,p.Generate,"job-runbutton"]}render(){const{collectionId:t,job:e}=this,{id:o}=e,[a,i]=this.jobStates?[this.jobStates[`${o}${u}`],this.jobStates[o]]:[void 0,void 0],[r,n,l]=this.jobStateToButtonProps(a),[c,d,h]=this.jobStateToButtonProps(i),b=t?"":"Select a source collection to enable this button";return s` <div class="card">
      <div class="card-body">
        <h2 class="card-title">${e.name}</h2>
        <p class="card-text">${e.description}</p>
        <div class="job-card-flex">
          <div class="job-card-sample">
            ${n===p.View?s`
                  <a
                    href="/datasets/${this.collectionId}:${o}?sample=true"
                    class="button ${l}"
                  >
                    ${r}
                  </a>
                `:s`
                  <button
                    class="job-button ${l}"
                    style="display: block"
                    data-job-id="${o}"
                    data-button-type="${n}"
                    data-sample=""
                    title="${b}"
                  >
                    ${r}
                  </button>
                `}
          </div>
          <div class="job-card-full">
            ${d===p.View?s`
                  <a
                    href="/datasets/${this.collectionId}:${o}"
                    class="button ${h}"
                  >
                    ${c}
                  </a>
                `:s`
                  <button
                    class="job-button ${h}"
                    style="display: block"
                    data-job-id="${o}"
                    data-button-type="${d}"
                    title="${b}"
                  >
                    ${c}
                  </button>
                `}
          </div>
        </div>
      </div>
    </div>`}};t([e()],b.prototype,"collectionId",void 0),t([e()],b.prototype,"job",void 0),t([e()],b.prototype,"jobStates",void 0),b=t([o("arch-job-card")],b);let m=class extends a{constructor(){super(...arguments),this.collapsed=!1}createRenderRoot(){return this}expand(){this.collapsed=!1}collapse(){this.collapsed=!0}render(){return s`
      <div class="job-category ${this.collapsed?"collapsed":"expanded"}">
        <button
          class="category-accordian-button"
          aria-controls=${this.jobsCat.categoryName}
          aria-expanded="${this.collapsed?"false":"true"}"
        >
          <img
            class="category-image"
            src="${this.jobsCat.categoryImage}"
            alt="Icon for ${this.jobsCat.categoryName}"
          />
          <span id="${this.jobsCat.categoryId}" class="category-title">
            ${this.jobsCat.categoryName}
          </span>
          <br />
          <span class="category-description">
            ${this.jobsCat.categoryDescription}
          </span>
        </button>
        <div id=${this.jobsCat.categoryName} class="collapsible-content">
          ${this.jobsCat.jobs.map((t=>s`
              <arch-job-card
                .collectionId=${this.collectionId}
                .job=${t}
                .jobStates=${this.jobStates}
              >
              </arch-job-card>
            `))}
        </div>
      </div>
    `}};t([e({type:Boolean})],m.prototype,"collapsed",void 0),t([e({type:String})],m.prototype,"collectionId",void 0),t([e({type:Object})],m.prototype,"jobsCat",void 0),t([e({type:Object})],m.prototype,"jobStates",void 0),m=t([o("arch-job-category-section")],m);var y,j=i``;let v=y=class extends a{constructor(){super(...arguments),this.collections=null,this.availableJobs=[],this.sourceCollectionId=null,this.jobStates={},this.activePollCollectionId=null,this.anyErrors=!1}async connectedCallback(){await this.initAvailableJobs(),this.initCollections(),super.connectedCallback(),this.addEventListener("click",(t=>{this.clickHandler(t)}))}createRenderRoot(){return this}render(){var t;const e=this.sourceCollectionId&&this.jobStates[this.sourceCollectionId];return s`
      <label for="source-collection">Select Source Collection</label>
      <select
        name="source-collection"
        @change=${this.sourceCollectionChangeHandler}
        ?disabled=${null===this.collections}
      >
        ${null===this.collections?s`<option>Loading...</option>`:s`<option value="">~ Choose Source Collection ~</option>`}
        ${(null!==(t=this.collections)&&void 0!==t?t:[]).map((t=>s`
            <option
              value="${t.id}"
              ?selected=${t.id===this.sourceCollectionId}
            >
              ${t.name}
            </option>
          `))}
      </select>

      <arch-alert
        class="sample"
        alertClass=${l.Secondary}
        message="Sample datasets can be quickly generated in order to ensure that the analysis will produce datasets that meet your needs. These datasets use the first 100 relative records from the collection if they are available. We strongly recommend generating samples for any collections over 100GB."
      ></arch-alert>

      <arch-alert
        class="email"
        alertClass=${l.Primary}
        message="ARCH is creating your dataset. You will receive an email notification when the dataset is complete."
        hidden
      ></arch-alert>

      <arch-alert
        class="error"
        alertClass=${l.Danger}
        message="A dataset generation job has failed, and we are currently investigating it."
        ?hidden=${!this.anyErrors}
      ></arch-alert>

      ${this.availableJobs.map(((t,o)=>s`
          <arch-job-category-section
            .collectionId=${this.sourceCollectionId}
            .jobsCat=${t}
            .jobStates=${e}
            ?collapsed=${o>0}
          >
          </arch-job-category-section>
        `))}
    `}setCollectionIdUrlParam(t){const{urlCollectionParamName:e}=y,o=new URL(window.location.href);t?o.searchParams.set(e,t):o.searchParams.delete(e),history.replaceState(null,"",o.toString())}async sourceCollectionChangeHandler(t){const e=t.target.value;this.setCollectionIdUrlParam(e),await this.setSourceCollectionId(e),this.requestUpdate()}updateAnyErrors(){const t=this.sourceCollectionId;if(t)for(const e of Object.values(this.jobStates[t]))if(e.state===h.Failed)return void(this.anyErrors=!0);this.anyErrors=!1}async setSourceCollectionId(t){this.sourceCollectionId=t,t&&(this.jobStates[t]=await this.fetchCollectionJobStates(t)),this.updateAnyErrors()}async initCollections(){this.collections=await(await fetch("/api/collections")).json();const t=new URLSearchParams(window.location.search).get(y.urlCollectionParamName);t&&(await this.setSourceCollectionId(t),this.requestUpdate())}async initAvailableJobs(){this.availableJobs=await(await fetch("/api/available-jobs")).json()}async fetchCollectionJobStates(t){const e=await(await fetch(`/api/jobstates/${t}?all=true`)).json();return Object.fromEntries(e.map((t=>[`${t.id}${t.sample>0?u:""}`,t])))}async pollJobStates(){const t=this.sourceCollectionId;if(this.activePollCollectionId===t){this.jobStates[t]=await this.fetchCollectionJobStates(t),this.updateAnyErrors(),this.requestUpdate();for(const e of Object.values(this.jobStates[t]))if(e.state===h.Running)return void setTimeout((()=>{this.pollJobStates()}),2e3);this.activePollCollectionId=null}else this.activePollCollectionId=null}startPolling(){null===this.activePollCollectionId&&(this.activePollCollectionId=this.sourceCollectionId,this.pollJobStates())}expandCategorySection(t){this.categorySections.forEach((e=>{e===t?e.expand():e.collapse()}))}async runJob(t,e){await fetch(`/api/runjob/${t}/${this.sourceCollectionId}${e?"?sample=true":""}`)}async clickHandler(t){const e=t.target,o=e.closest("arch-job-category-section");if(null==o?void 0:o.collapsed)this.expandCategorySection(o);else if("BUTTON"!==e.tagName);else{const{jobId:t,buttonType:o,sample:a}=e.dataset,s=void 0!==a;switch(o){case p.Generate:await this.runJob(t,s),this.emailAlert.show(),this.startPolling();break;case p.View:window.location.href=`/datasets/${this.sourceCollectionId}:${t}${s?"?sample=true":""}`}}}};v.styles=j,v.urlCollectionParamName="cid",t([n()],v.prototype,"collections",void 0),t([n()],v.prototype,"availableJobs",void 0),t([n()],v.prototype,"sourceCollectionId",void 0),t([n()],v.prototype,"jobStates",void 0),t([n()],v.prototype,"activePollCollectionId",void 0),t([n()],v.prototype,"anyErrors",void 0),t([r("select[name=source-collection]")],v.prototype,"collectionSelector",void 0),t([r("arch-alert.error")],v.prototype,"errorAlert",void 0),t([r("arch-alert.email")],v.prototype,"emailAlert",void 0),t([c("arch-job-category-section")],v.prototype,"categorySections",void 0),v=y=t([o("arch-generate-dataset-form")],v);export{v as A,h as P};
//# sourceMappingURL=chunk-arch-generate-dataset-form-94c66d79.js.map
