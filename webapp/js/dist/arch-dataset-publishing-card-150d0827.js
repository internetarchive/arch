import{g as t,i as a,h as i,_ as e,e as s,c as d,b as n,y as o,a as r}from"./chunk-styles-75502ec5.js";import{t as l}from"./chunk-arch-alert-a83c3a9d.js";import{i as h}from"./chunk-helpers-139f8162.js";import"./arch-sub-collection-builder-cd409a8e.js";import"./chunk-arch-generate-dataset-form-6a3e9363.js";import"./chunk-arch-loading-indicator-37c0007d.js";import{_ as c}from"./chunk-arch-dataset-metadata-form-8b222489.js";import"./chunk-query-all-273a2103.js";var u=[t,a`
    :host > div.container {
      display: flex;
    }

    :host > div.container > div:first-child {
      flex-grow: 1;
    }

    :host > div.container > button {
      align-self: flex-start;
    }

    :host > div.container > button.cancel {
      margin-right: 0.5rem;
    }

    h2 {
      font-size: 1em;
      margin: 0 0 0.75em 0;
    }

    /* Prevent items from overflow container: https://stackoverflow.com/a/66689926 */
    div.detail {
      min-width: 0;
    }

    div.metadata-display > dl,
    div.metadata-display > arch-loading-indicator,
    div.metadata-display > i {
      margin-left: 2rem;
    }

    div.metadata-edit {
      background-color: ${i};
      border-radius: 8px;
      padding: 1rem 1.5rem;
    }

    dl > div,
    dl > div:last-child {
      margin-bottom: 0.75em;
    }

    [hidden] {
      display: none;
    }

    div.form-buttons {
      text-align: right;
    }
  `];const m=c,b=m.propertiesOrder;var p,g;!function(t){t[t.Loading=0]="Loading",t[t.Unpublished=1]="Unpublished",t[t.PrePublish=2]="PrePublish",t[t.Publishing=3]="Publishing",t[t.Published=4]="Published",t[t.Unpublishing=5]="Unpublishing"}(p||(p={})),function(t){t[t.Displaying=0]="Displaying",t[t.Editing=1]="Editing",t[t.Saving=2]="Saving"}(g||(g={}));const f=Object.keys(m.properties).sort(((t,a)=>b.indexOf(t)<b.indexOf(a)?-1:1));let v=class extends n{constructor(){super(...arguments),this.pubState=p.Loading,this.pubInfo=void 0,this.metadataState=g.Displaying,this.metadata=void 0}connectedCallback(){super.connectedCallback(),this._fetchInitialData()}get _sampleParam(){return"sample="+(this.isSample?"true":"false")}get _metadataFormData(){var t;const a={},i=Array.from(new FormData(this.metadataForm.form).entries()).filter((([,t])=>""!==t.trim())).map((([t,a])=>[t,a.replaceAll("\t"," ").replaceAll("\n","<br>")]));for(const[e,s]of i)a[e]=(null!==(t=a[e])&&void 0!==t?t:[]).concat(s);return a}render(){const{pubState:t}=this;if(t===p.Loading)return o`<arch-loading-indicator></arch-loading-indicator>`;const{metadata:a}=this,i=this.pubInfo;return o`
      <div class="container">
        <div class="detail">
          <dl>
            <div>
              <dt>Last Published</dt>
              <dd>
                ${t===p.Published?h(i.time):"never"}
              </dd>
            </div>
            ${t!==p.Published?o``:o`
                  <div>
                    <dt>ARK</dt>
                    <dd>
                      <a href="https://ark.archive.org/${i.ark}"
                        >${i.ark}</a
                      >
                    </dd>
                  </div>
                `}
          </dl>

          <!-- Metadata section header -->
          <h2>
            ${t<p.PrePublish||t===p.Publishing?"":t===p.PrePublish?o`<i>Enter Metadata</i>`:"Metadata"}
            ${t<p.Published||this.metadataState===g.Editing?"":o`
                  <button
                    class="text"
                    @click=${()=>this.metadataState=g.Editing}
                  >
                    (edit)
                  </button>
                `}
          </h2>

          <!-- Metadata display list -->
          <div
            class="metadata-display"
            ?hidden=${t<p.Published||this.metadataState===g.Editing}
          >
            ${void 0===a?o`<arch-loading-indicator></arch-loading-indicator>`:0===Object.keys(a).length?o`<i>none</i>`:o`
                  <dl>
                    ${f.filter((t=>void 0!==a[t])).map((t=>{const i=function(t){return m.properties[t].title}(t);let e=a[t];return Array.isArray(e)||(e=[e]),o`
                          <div>
                            <dt>${i}</dt>
                            ${e.map((t=>o`<dd>${t}</dd>`))}
                          </div>
                        `}))}
                  </dl>
                `}
          </div>

          <!-- Metadata edit form -->
          <div
            class="metadata-edit"
            ?hidden=${t!==p.PrePublish&&this.metadataState!==g.Editing&&this.metadataState!==g.Saving}
          >
            ${t!==p.PrePublish&&this.metadataState!==g.Editing&&this.metadataState!==g.Saving?o``:o`
                  <arch-dataset-metadata-form
                    metadata="${JSON.stringify(null!=a?a:"")}"
                  >
                  </arch-dataset-metadata-form>
                `}
            <br />
            <div
              ?hidden=${t===p.PrePublish}
              class="form-buttons"
            >
              <button
                type="button"
                @click=${()=>this.metadataState=g.Displaying}
                ?disabled=${this.metadataState===g.Saving}
              >
                Cancel
              </button>
              <button
                type="button"
                class="primary"
                @click=${()=>this._saveMetadata()}
                ?disabled=${this.metadataState===g.Saving}
              >
                ${this.metadataState===g.Saving?o`<arch-loading-indicator
                      style="--color: #fff"
                      text="Saving"
                    ></arch-loading-indicator>`:o`Save`}
              </button>
            </div>
          </div>
        </div>

        <button
          class="cancel"
          @click=${()=>this.pubState=p.Unpublished}
          ?hidden=${t!==p.PrePublish}
        >
          Cancel
        </button>

        <button
          class="${t===p.Unpublished?"primary":t===p.PrePublish?"success":t===p.Published?"danger":""}"
          ?disabled=${t===p.Publishing||t===p.Unpublishing}
          @click=${this._publishButtonClickHandler}
        >
          ${t===p.Unpublished?"Publish":t===p.PrePublish?"Publish Now":t===p.Publishing?"Publish in progress...":t===p.Published?"Unpublish":t===p.Unpublishing?"Unpublishing...":""}
        </button>
      </div>
    `}async _fetchInitialData(){const t=await this._fetchPubInfo();if(t)this.pubInfo=t,this.pubState=p.Published,this._pollItemMetadata();else{await this._publishInProgress()?(this.pubState=p.Publishing,setTimeout((()=>{this._fetchInitialData()}),3e3)):(this.pubState=p.Unpublished,this.metadata={})}}async _pollItemMetadata(){const{pubState:t}=this,a=this.pubInfo,i=await this._fetchItemMetadata(a.item);void 0===i&&t===p.Published&&setTimeout((()=>{this._pollItemMetadata()}),3e3),this.metadata=i}async _fetchPubInfo(){const t=await fetch(`/api/petabox/${this.collectionId}/${this.jobId}?${this._sampleParam}`);if(404!==t.status){const a=await t.json();return a.time=new Date(a.time),a}}async _publishInProgress(){var t,a;const{collectionId:i}=this,e=await(await fetch(`/api/jobstate/DatasetPublication/${i}?${this._sampleParam}`)).json(),s=Date.parse(null!==(t=e.startTime)&&void 0!==t?t:""),d=Date.parse(null!==(a=e.finishedTime)&&void 0!==a?a:"");return!Number.isNaN(s)&&(!!Number.isNaN(d)||s>d)}async _fetchItemMetadata(t){const a=await fetch(`/api/petabox/${this.collectionId}/metadata/${t}`);if(404!==a.status)return await a.json()}_publishButtonClickHandler(){const t=this.metadataForm;switch(this.pubState){case p.Unpublished:this.pubState=p.PrePublish;break;case p.PrePublish:t.form.checkValidity()?this._publish():t.form.reportValidity();break;case p.Published:window.confirm("Are you sure you want to unpublish this dataset?")&&this._unpublish()}}async _publish(){const{collectionId:t,jobId:a,_metadataFormData:i}=this;await fetch(`/api/runjob/DatasetPublication/${t}?${this._sampleParam}`,{method:"POST",credentials:"same-origin",mode:"cors",body:JSON.stringify({dataset:a,metadata:i})}),this.pubState=p.Publishing,this._fetchInitialData()}async _unpublish(){const{collectionId:t,pubInfo:a}=this,{item:i}=a;this.pubState=p.Unpublishing,await fetch(`/api/petabox/${t}/delete/${i}`,{method:"POST",credentials:"same-origin",mode:"cors",body:JSON.stringify({delete:!0})}),this.pubState=p.Unpublished,this._fetchInitialData()}async _saveMetadata(){const{collectionId:t,pubInfo:a,_metadataFormData:i}=this,{item:e}=a;this.metadata=i,this.metadataState=g.Saving;const s=Object.assign(Object.fromEntries(f.map((t=>[t,[]]))),i);await fetch(`/api/petabox/${t}/metadata/${e}`,{method:"POST",credentials:"same-origin",mode:"cors",body:JSON.stringify(s)}),this.metadataState=g.Displaying}};v.styles=u,e([s({type:String})],v.prototype,"collectionId",void 0),e([s({type:String})],v.prototype,"jobId",void 0),e([s({type:Boolean})],v.prototype,"isSample",void 0),e([l()],v.prototype,"pubState",void 0),e([l()],v.prototype,"pubInfo",void 0),e([l()],v.prototype,"metadataState",void 0),e([l()],v.prototype,"metadata",void 0),e([d("arch-dataset-metadata-form")],v.prototype,"metadataForm",void 0),v=e([r("arch-dataset-publishing-card")],v);export{v as ArchDatasetPublishingCard};
//# sourceMappingURL=arch-dataset-publishing-card-150d0827.js.map
