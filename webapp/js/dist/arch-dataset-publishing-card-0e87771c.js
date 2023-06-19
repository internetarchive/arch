import{g as t,i as a,_ as i,e,b as s,s as d,y as n,a as o}from"./chunk-styles-690ba0e4.js";import{t as r}from"./chunk-arch-alert-46aea875.js";import{i as l}from"./chunk-helpers-139f8162.js";import"./arch-sub-collection-builder-2c0fad15.js";import"./arch-generate-dataset-form-e492768c.js";import{a as h}from"./chunk-types-df9f20ed.js";import"./chunk-arch-loading-indicator-02b42d35.js";import{p as c}from"./chunk-arch-dataset-metadata-form-24107ff5.js";var m,u,b=[t,a`
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
    div.metadata-display > i,
    div.metadata-edit {
      margin-left: 2rem;
    }

    dl > div,
    dl > div:last-child {
      margin-bottom: 0.75em;
    }

    [hidden] {
      display: none;
    }
  `];!function(t){t[t.Loading=0]="Loading",t[t.Unpublished=1]="Unpublished",t[t.PrePublish=2]="PrePublish",t[t.Publishing=3]="Publishing",t[t.Published=4]="Published"}(m||(m={})),function(t){t[t.Displaying=0]="Displaying",t[t.Editing=1]="Editing",t[t.Saving=2]="Saving"}(u||(u={}));const p=Array.from(Object.keys(h));let f=class extends d{constructor(){super(...arguments),this.pubState=m.Loading,this.pubInfo=void 0,this.metadataState=u.Displaying,this.metadata=void 0}connectedCallback(){super.connectedCallback(),this._fetchInitialData()}get _sampleParam(){return"sample="+(this.isSample?"true":"false")}get _metadataFormData(){var t;const a={},i=Array.from(new FormData(this.metadataForm.form).entries());for(let[e,s]of i)s=s.replaceAll("\t"," ").replaceAll("\n","<br>"),a[e]=(null!==(t=a[e])&&void 0!==t?t:[]).concat(s);return a}render(){const{pubState:t}=this;if(t===m.Loading)return n`<arch-loading-indicator></arch-loading-indicator>`;const{metadata:a}=this,i=this.pubInfo;return n`
      <div class="container">
        <div class="detail">
          <dl>
            <div>
              <dt>Last Published</dt>
              <dd>
                ${t===m.Published?l(i.time):"never"}
              </dd>
            </div>
            ${t!==m.Published?n``:n`
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
            ${t<m.PrePublish||t===m.Publishing?"":t===m.PrePublish?n`<i>Enter Metadata</i>`:"Metadata"}
            ${t<m.Published||this.metadataState===u.Editing?"":n`
                  <button
                    class="text"
                    @click=${()=>this.metadataState=u.Editing}
                  >
                    (edit)
                  </button>
                `}
          </h2>

          <!-- Metadata display list -->
          <div
            class="metadata-display"
            ?hidden=${t<m.Published||this.metadataState===u.Editing}
          >
            ${void 0===a?n`<arch-loading-indicator></arch-loading-indicator>`:0===Object.keys(a).length?n`<i>none</i>`:n`
                  <dl>
                    ${p.filter((t=>void 0!==a[t])).map((t=>{const i=function(t){return c[t].title}(t);let e=a[t];return Array.isArray(e)||(e=[e]),n`
                          <div>
                            <dt>${i}</dt>
                            ${e.map((t=>n`<dd>${t}</dd>`))}
                          </div>
                        `}))}
                  </dl>
                `}
          </div>

          <!-- Metadata edit form -->
          <div
            class="metadata-edit"
            ?hidden=${t!==m.PrePublish&&this.metadataState!==u.Editing}
          >
            ${t!==m.PrePublish&&this.metadataState!==u.Editing?n``:n`
                  <arch-dataset-metadata-form
                    metadata="${JSON.stringify(a||{})}"
                  >
                  </arch-dataset-metadata-form>
                `}
            <br />
            <span ?hidden=${t===m.PrePublish}>
              <button
                type="button"
                @click=${()=>this.metadataState=u.Displaying}
                ?disabled=${this.metadataState===u.Saving}
              >
                Cancel
              </button>
              <button
                type="button"
                class="primary"
                @click=${()=>this._saveMetadata()}
                ?disabled=${this.metadataState===u.Saving}
              >
                Save
              </button>
            </span>
          </div>
        </div>

        <button
          class="cancel"
          @click=${()=>this.pubState=m.Unpublished}
          ?hidden=${t!==m.PrePublish}
        >
          Cancel
        </button>

        <button
          class="${t===m.Unpublished?"primary":t===m.PrePublish?"success":""}"
          ?disabled=${t!==m.Unpublished&&t!==m.PrePublish}
          @click=${this._buttonClickHandler}
        >
          ${t===m.Unpublished?"Publish":t===m.PrePublish?"Publish Now":t===m.Publishing?"Publish in progress...":"Published"}
        </button>
      </div>
    `}async _fetchInitialData(){const t=await this._fetchPubInfo();if(t)this.pubInfo=t,this.pubState=m.Published,this._pollItemMetadata();else{await this._publishInProgress()?(this.pubState=m.Publishing,setTimeout((()=>{this._fetchInitialData()}),3e3)):this.pubState=m.Unpublished}}async _pollItemMetadata(){const t=this.pubInfo,a=await this._fetchItemMetadata(t.item);void 0===a&&setTimeout((()=>{this._pollItemMetadata()}),3e3),this.metadata=a}async _fetchPubInfo(){const t=await fetch(`/api/petabox/${this.collectionId}/${this.jobId}?${this._sampleParam}`);if(404!==t.status){const a=await t.json();return a.time=new Date(a.time),a}}async _publishInProgress(){var t,a;const{collectionId:i}=this,e=await(await fetch(`/api/jobstate/DatasetPublication/${i}?${this._sampleParam}`)).json(),s=Date.parse(null!==(t=e.startTime)&&void 0!==t?t:""),d=Date.parse(null!==(a=e.finishedTime)&&void 0!==a?a:"");return!Number.isNaN(s)&&(!!Number.isNaN(d)||s>d)}async _fetchItemMetadata(t){const a=await fetch(`/api/petabox/${this.collectionId}/metadata/${t}`);if(404!==a.status)return await a.json()}_buttonClickHandler(){const{metadataForm:t}=this;switch(this.pubState){case m.Unpublished:this.pubState=m.PrePublish;break;case m.PrePublish:t.form.checkValidity()?this._publish():t.form.reportValidity()}}async _publish(){const{collectionId:t,jobId:a,_metadataFormData:i}=this;await fetch(`/api/runjob/DatasetPublication/${t}?${this._sampleParam}`,{method:"POST",credentials:"same-origin",mode:"cors",body:JSON.stringify({dataset:a,metadata:i})}),this.pubState=m.Publishing,this._fetchInitialData()}async _saveMetadata(){const{collectionId:t,pubInfo:a,_metadataFormData:i}=this,{item:e}=a;this.metadata=i,this.metadataState=u.Saving;const s=Object.assign(Object.fromEntries(p.map((t=>[t,[]]))),i);await fetch(`/api/petabox/${t}/metadata/${e}`,{method:"POST",credentials:"same-origin",mode:"cors",body:JSON.stringify(s)}),this.metadataState=u.Displaying}};f.styles=b,i([e({type:String})],f.prototype,"collectionId",void 0),i([e({type:String})],f.prototype,"jobId",void 0),i([e({type:Boolean})],f.prototype,"isSample",void 0),i([r()],f.prototype,"pubState",void 0),i([r()],f.prototype,"pubInfo",void 0),i([r()],f.prototype,"metadataState",void 0),i([r()],f.prototype,"metadata",void 0),i([s("arch-dataset-metadata-form")],f.prototype,"metadataForm",void 0),f=i([o("arch-dataset-publishing-card")],f);export{f as ArchDatasetPublishingCard};
//# sourceMappingURL=arch-dataset-publishing-card-0e87771c.js.map
