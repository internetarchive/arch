import{g as t,j as a,i as e,_ as s,b as r,y as d,a as i}from"./chunk-styles-ad31501a.js";import{t as l}from"./chunk-state-1d3d2492.js";import{a as o}from"./chunk-arch-alert-ef577355.js";import{P as c}from"./chunk-helpers-e724cca8.js";import{i as n}from"./chunk-arch-loading-indicator-2842cdb6.js";import"./chunk-arch-card-ad3a7cc0.js";import"./arch-sub-collection-builder-46e3dce6.js";import"./chunk-arch-generate-dataset-form-22ef0d7d.js";import"./chunk-query-all-dcc1a25c.js";import"./chunk-scale-large-891207ee.js";var h,m=[t,a,e`
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
  `];let p=h=class extends r{constructor(){super(),this.numTotalDatasets=0,this.datasets=void 0,this.initDatasets()}render(){var t,a;const{numTotalDatasets:e}=this,s=void 0===this.datasets,r=(null!==(t=this.datasets)&&void 0!==t?t:[]).length>0,i=null!==(a=this.datasets)&&void 0!==a?a:[];return d`
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
              ${s?[d`<tr>
              <td colspan="3">
                <arch-loading-indicator></arch-loading-indicator>
              </td>
            </tr>`]:r?i.map((t=>{const a=`${t.name}${-1!==t.sample?" (Sample)":""}`;return d`
              <tr>
                <td class="name">
                  <a
                    href="${c.dataset(t.id,t.sample)}"
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
            `})):[d`<tr>
              <td colspan="3"><i>New datasets will be listed here.</i></td>
            </tr>`]}
            </tbody>
          </table>
        </div>
        <div slot="footer">
          ${s||!r?d``:d`
                <a href="/datasets/explore" class="view-all">
                  View
                  ${i.length<e?d`All ${e}`:d``}
                  Datasets
                </a>
              `}
        </div>
      </arch-card>
    `}async initDatasets(){const t=await o.datasets.get([["state","=","Finished"],["sort","=","-startTime"],["limit","=",h.maxDisplayedDatasets]]);this.numTotalDatasets=t.count,this.datasets=t.results}};p.maxDisplayedDatasets=10,p.styles=m,s([l()],p.prototype,"numTotalDatasets",void 0),s([l()],p.prototype,"datasets",void 0),p=h=s([i("arch-recent-datasets-card")],p);export{p as ArchRecentDatasetsCard};
//# sourceMappingURL=arch-recent-datasets-card-f61f488b.js.map
