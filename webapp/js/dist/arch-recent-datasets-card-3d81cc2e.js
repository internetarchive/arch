import{g as t,j as a,i as e,_ as s,b as r,y as i,a as d}from"./chunk-styles-48eb3c3c.js";import{t as c}from"./chunk-state-b70c226d.js";import{a as l}from"./chunk-arch-alert-84f9ff63.js";import{P as o}from"./chunk-helpers-7d3afcd3.js";import{i as n}from"./chunk-arch-loading-indicator-93cca752.js";import"./chunk-arch-card-116a2dc2.js";import"./arch-sub-collection-builder-b75d93c9.js";import"./chunk-arch-generate-dataset-form-a4fc78d3.js";import"./chunk-query-all-ac9cba43.js";import"./chunk-scale-large-c2ff54fd.js";var h,m=[t,a,e`
    thead > tr.hidden-header {
      color: transparent;
    }

    th.date {
      width: 8rem;
      text-align: right;
    }

    td.name,
    td.collection {
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow-x: hidden;
    }

    td.date {
      text-align: right;
    }
  `];let p=h=class extends r{constructor(){super(),this.numTotalDatasets=0,this.datasets=void 0,this.initDatasets()}render(){var t,a;const{numTotalDatasets:e}=this,s=void 0===this.datasets,r=(null!==(t=this.datasets)&&void 0!==t?t:[]).length>0,d=null!==(a=this.datasets)&&void 0!==a?a:[];return i`
      <arch-card
        title="Recent Datasets"
        ctatext="Generate New Dataset"
        ctahref="/datasets/generate"
      >
        <div slot="content">
          <table>
            <thead>
              <tr class="${s||!r?"hidden-header":""}">
                <th class="name">Dataset</th>
                <th class="collection">Collection Name</th>
                <th class="date">Date Generated</th>
              </tr>
            </thead>
            <tbody>
              ${s?[i`<tr>
              <td colspan="3">
                <arch-loading-indicator></arch-loading-indicator>
              </td>
            </tr>`]:r?d.map((t=>{const a=`${t.name}${-1!==t.sample?" (Sample)":""}`;return i`
              <tr>
                <td class="name">
                  <a
                    href="${o.dataset(t.id,t.sample)}"
                    title="${a}"
                  >
                    ${a}
                  </a>
                </td>
                <td class="collection" title="${t.collectionName}">
                  ${t.collectionName}
                </td>
                <td class="date">
                  ${n(t.finishedTime)}
                </td>
              </tr>
            `})):[i`<tr>
              <td colspan="3"><i>New datasets will be listed here.</i></td>
            </tr>`]}
            </tbody>
          </table>
        </div>
        <div slot="footer">
          ${s||!r?i``:i`
                <a href="/datasets/explore" class="view-all">
                  View
                  ${d.length<e?i`All ${e}`:i``}
                  Datasets
                </a>
              `}
        </div>
      </arch-card>
    `}async initDatasets(){const t=await l.datasets.get([["state","=","Finished"],["sort","=","-startTime"],["limit","=",h.maxDisplayedDatasets]]);this.numTotalDatasets=t.count,this.datasets=t.results}};p.maxDisplayedDatasets=10,p.styles=m,s([c()],p.prototype,"numTotalDatasets",void 0),s([c()],p.prototype,"datasets",void 0),p=h=s([d("arch-recent-datasets-card")],p);export{p as ArchRecentDatasetsCard};
//# sourceMappingURL=arch-recent-datasets-card-3d81cc2e.js.map
