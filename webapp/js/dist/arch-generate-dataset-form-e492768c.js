import{_ as t,e,a as o,s as a,y as s,i as l,b as i}from"./chunk-styles-690ba0e4.js";import{t as c,A as r}from"./chunk-arch-alert-46aea875.js";import{e as n,P as d}from"./chunk-types-df9f20ed.js";const h="-SAMPLE";var b;!function(t){t.Generate="generate",t.View="view",t.Status="status"}(b||(b={}));let p=class extends a{createRenderRoot(){return this}jobStateToButtonProps(t){if(!t)return[this.collectionId?"Loading...":"n/a",b.Status,"job-statebutton"];const e=t.sample>0?"Sample ":"";return t.started?t.finished?[`View ${e}Dataset`,b.View,"job-resultsbutton"]:[t.state,b.Status,"job-statebutton"]:[`Generate ${e}Dataset`,b.Generate,"job-runbutton"]}render(){const{collectionId:t,job:e}=this,{id:o}=e,[a,l]=this.jobStates?[this.jobStates[`${o}${h}`],this.jobStates[o]]:[void 0,void 0],[i,c,r]=this.jobStateToButtonProps(a),[n,d,p]=this.jobStateToButtonProps(l),u=t?"":"Select a source collection to enable this button";return s` <div class="card">
      <div class="card-body">
        <h3 class="card-title">${e.name}</h3>
        <p class="card-text">${e.description}</p>
        <div class="job-card-flex">
          <div class="job-card-sample">
            ${c===b.View?s`
                  <a
                    href="/datasets/${this.collectionId}:${o}?sample=true"
                    class="button ${r}"
                  >
                    ${i}
                  </a>
                `:s`
                  <button
                    class="job-button ${r}"
                    style="display: block"
                    data-job-id="${o}"
                    data-button-type="${c}"
                    data-sample=""
                    title="${u}"
                  >
                    ${i}
                  </button>
                `}
          </div>
          <div class="job-card-full">
            ${d===b.View?s`
                  <a
                    href="/datasets/${this.collectionId}:${o}"
                    class="button ${p}"
                  >
                    ${n}
                  </a>
                `:s`
                  <button
                    class="job-button ${p}"
                    style="display: block"
                    data-job-id="${o}"
                    data-button-type="${d}"
                    title="${u}"
                  >
                    ${n}
                  </button>
                `}
          </div>
        </div>
      </div>
    </div>`}};t([e()],p.prototype,"collectionId",void 0),t([e()],p.prototype,"job",void 0),t([e()],p.prototype,"jobStates",void 0),p=t([o("arch-job-card")],p);let u=class extends a{constructor(){super(...arguments),this.collapsed=!1}createRenderRoot(){return this}connectedCallback(){this.setAttribute("aria-controls",this.jobsCat.categoryName),super.connectedCallback()}expand(){this.collapsed=!1}collapse(){this.collapsed=!0}render(){return s`
      <div
        class="job-category ${this.collapsed?"collapsed":"expanded"}"
        aria-expanded="${this.collapsed?"false":"true"}"
      >
        <div class="category-wrapper">
          <img
            class="category-image"
            src="${this.jobsCat.categoryImage}"
            alt="Icon for ${this.jobsCat.categoryName}"
          />
          <h2 id="${this.jobsCat.categoryId}" class="category-title">
            ${this.jobsCat.categoryName}
          </h2>
          <p>${this.jobsCat.categoryDescription}</p>
        </div>
        <div class="collapsible-content">
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
    `}};t([e({type:Boolean})],u.prototype,"collapsed",void 0),t([e({type:String})],u.prototype,"collectionId",void 0),t([e({type:Object})],u.prototype,"jobsCat",void 0),t([e({type:Object})],u.prototype,"jobStates",void 0),u=t([o("arch-job-category-section")],u);var y,j=l``;let v=y=class extends a{constructor(){super(...arguments),this.collections=null,this.availableJobs=[],this.sourceCollectionId=null,this.jobStates={},this.activePollCollectionId=null,this.anyErrors=!1}async connectedCallback(){await this.initAvailableJobs(),this.initCollections(),super.connectedCallback(),this.addEventListener("click",(t=>{this.clickHandler(t)}))}createRenderRoot(){return this}render(){var t;const e=this.sourceCollectionId&&this.jobStates[this.sourceCollectionId];return s`
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
        alertClass=${r.Secondary}
        message="Sample datasets can be quickly generated in order to ensure that the analysis will produce datasets that meet your needs. These datasets use the first 100 relative records from the collection if they are available. We strongly recommend running sample jobs on any collections over 100GB."
      ></arch-alert>

      <arch-alert
        class="email"
        alertClass=${r.Primary}
        message="Your job has been queued. An e-mail will be sent once it is complete."
        hidden
      ></arch-alert>

      <arch-alert
        class="error"
        alertClass=${r.Danger}
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
    `}setCollectionIdUrlParam(t){const{urlCollectionParamName:e}=y,o=new URL(window.location.href);t?o.searchParams.set(e,t):o.searchParams.delete(e),history.replaceState(null,"",o.toString())}async sourceCollectionChangeHandler(t){const e=t.target.value;this.setCollectionIdUrlParam(e),await this.setSourceCollectionId(e),this.requestUpdate()}updateAnyErrors(){const t=this.sourceCollectionId;if(t)for(const e of Object.values(this.jobStates[t]))if(e.state===d.Failed)return void(this.anyErrors=!0);this.anyErrors=!1}async setSourceCollectionId(t){this.sourceCollectionId=t,t&&(this.jobStates[t]=await this.fetchCollectionJobStates(t)),this.updateAnyErrors()}async initCollections(){this.collections=await(await fetch("/api/collections")).json();const t=new URLSearchParams(window.location.search).get(y.urlCollectionParamName);t&&(await this.setSourceCollectionId(t),this.requestUpdate())}async initAvailableJobs(){this.availableJobs=await(await fetch("/api/available-jobs")).json()}async fetchCollectionJobStates(t){const e=await(await fetch(`/api/jobstates/${t}?all=true`)).json();return Object.fromEntries(e.map((t=>[`${t.id}${t.sample>0?h:""}`,t])))}async pollJobStates(){const t=this.sourceCollectionId;if(this.activePollCollectionId===t){this.jobStates[t]=await this.fetchCollectionJobStates(t),this.updateAnyErrors(),this.requestUpdate();for(const e of Object.values(this.jobStates[t]))if(e.state===d.Running)return void setTimeout((()=>{this.pollJobStates()}),2e3);this.activePollCollectionId=null}else this.activePollCollectionId=null}startPolling(){null===this.activePollCollectionId&&(this.activePollCollectionId=this.sourceCollectionId,this.pollJobStates())}expandCategorySection(t){this.categorySections.forEach((e=>{e===t?e.expand():e.collapse()}))}async runJob(t,e){await fetch(`/api/runjob/${t}/${this.sourceCollectionId}${e?"?sample=true":""}`)}async clickHandler(t){const e=t.target,o=e.closest("arch-job-category-section");if(null==o?void 0:o.collapsed)this.expandCategorySection(o);else if("BUTTON"!==e.tagName);else{const{jobId:t,buttonType:o,sample:a}=e.dataset,s=void 0!==a;switch(o){case b.Generate:await this.runJob(t,s),this.emailAlert.show(),this.startPolling();break;case b.View:window.location.href=`/datasets/${this.sourceCollectionId}:${t}${s?"?sample=true":""}`}}}};v.styles=j,v.urlCollectionParamName="cid",t([c()],v.prototype,"collections",void 0),t([c()],v.prototype,"availableJobs",void 0),t([c()],v.prototype,"sourceCollectionId",void 0),t([c()],v.prototype,"jobStates",void 0),t([c()],v.prototype,"activePollCollectionId",void 0),t([c()],v.prototype,"anyErrors",void 0),t([i("select[name=source-collection]")],v.prototype,"collectionSelector",void 0),t([i("arch-alert.error")],v.prototype,"errorAlert",void 0),t([i("arch-alert.email")],v.prototype,"emailAlert",void 0),t([n("arch-job-category-section")],v.prototype,"categorySections",void 0),v=y=t([o("arch-generate-dataset-form")],v);export{v as ArchGenerateDatasetForm};
//# sourceMappingURL=arch-generate-dataset-form-e492768c.js.map
