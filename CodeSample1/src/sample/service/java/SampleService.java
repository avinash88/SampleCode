package sample.service.java;
package com.*******r;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.*****;

//import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;


public class SampleService {
	
	private MedsTemplateDAO medsTemplateDAO;
	private TemplateExceptionDAO templateExceptionDAO;

	
	private static Logger log = Logger.getLogger(PlanningManager.class);
	
	
	public List selectMedsTemplate() throws Exception {
		
		List list = new ArrayList();
		
		Ehcache cache = cacheManager.getEhcache("med-template-cache");
		Element element = cache.get("meds-template-key");
		
		if(element == null){
			List<MedsTemplate> listDAO = this.getMedsTemplateDAO().selectMedsTemplate();		
			MedsTemplate data = null;
			MedsTemplate last = null;
			boolean fourByFour = false;
			boolean lastFourByTwo = false;
	
			boolean b4ft = false;
			boolean b6ft = false;
			boolean b8ft = false;
			boolean b4to8ft = false;
			boolean last12ft = false;
			
			for (Iterator<MedsTemplate> itr = listDAO.iterator(); itr.hasNext(); ) {
				data = itr.next();			
				fourByFour = (data.getTemplateNm().indexOf("4x4") > 0);
				b4ft = (data.getTemplateNm().indexOf("4 FOOT") > 0);
				b6ft = (data.getTemplateNm().indexOf("6 FOOT") > 0);
				b8ft = (data.getTemplateNm().indexOf("8 FOOT") > 0);
				b4to8ft = (b4ft || b6ft || b8ft);
				
				if(list.size() > 0) {
					last = (MedsTemplate) list.get(list.size()-1);			
					lastFourByTwo = (last.getTemplateNm().indexOf("4x2") > 0);
					
					last12ft = (last.getTemplateNm().indexOf("12 FOOT") > 0);
				}
				
				if(fourByFour && lastFourByTwo // put 4x4 above 4x2
						|| b4to8ft && last12ft){ // put 4/6/8ft above 12 ft
					list.remove(list.size()-1);
					list.add(data);
					list.add(last);
				} else {
					list.add(data);
				}
			}
			
			Element elep = new Element("meds-template-key", list);
			cache.put(elep);
			//log.info("MedsTemplate from db");
		} else{
			list = (List)element.getValue();
			//log.info("MedsTemplate from cache");
		}

		return list;
	}
	
	public String getMedsTemplateId(String medName, String medNo) throws Exception {
		
		String med_id = "";
		MedsTemplate medSelected = null;
		String med_name = "";
		String med_no = "";
		List list = this.selectMedsTemplate();
			
		for (Iterator<MedsTemplate> itr = list.iterator(); itr.hasNext(); ) {
			medSelected = itr.next();

			med_name = this.getReportName(medSelected.getReportId());
			med_no = medSelected.getReportNo();

			// Get med_id
			if (("Beverage".equalsIgnoreCase(med_name)) || ("Family care".equalsIgnoreCase(med_name)) 
					|| ("Endcap".equalsIgnoreCase(med_name)) 
					|| ("Hawaii Asian 4x2".equalsIgnoreCase(med_name)) || ("Hawaii Asian 4x4".equalsIgnoreCase(med_name))
					|| ("Pet Meds 4x2".equalsIgnoreCase(med_name)) || ("Pet Meds 4x4".equalsIgnoreCase(med_name)))
			{
				//log.info("matched");				
				if (medName.equalsIgnoreCase(med_name)){
					med_id = medSelected.getTemplateId().toString();
					break;
				}
			} else { // frozen, lads-meds, priority, BSG, Bunker, Liquid MED, NP Meat Bunker, natural food, lowboy, Energy Drink, Wall of Value
				 if(medName.equalsIgnoreCase(med_name) && medNo.equalsIgnoreCase(med_no)) {
					med_id = medSelected.getTemplateId().toString();
					//log.info("Found med, name="+med_name+" no="+med_no + " id ="+med_id);
					break;
				 }
			}
		}

		return med_id;
	}
	
	public List selectMedsTemplateByReportId(Integer report_id)
		throws Exception {
		log.debug("MedsTemplateDAO-->" + this.getMedsTemplateDAO());
		
		List list = null;
		List listReturn = new ArrayList();
		MedsTemplate data = null;
		
		list = this.getMedsTemplateDAO().selectMedsTemplateByReportId(report_id);
		
		for (Iterator itr = list.iterator(); itr.hasNext(); ) {
			data = (MedsTemplate) itr.next();
			
			String current = data.getReportNo();
			if (current.length() < 2) current = "0"+current;
			
			if(listReturn.size() > 0){ 
				MedsTemplate lastTemp = (MedsTemplate) listReturn.get(listReturn.size()-1);
				String last = lastTemp.getReportNo();
				if (last.length() < 2) last = "0"+last;
				
				if(last.compareTo(current) > 0){
					listReturn.remove(listReturn.size()-1);
					listReturn.add(data);
					listReturn.add(lastTemp);
				} else {
					listReturn.add(data);
				}
			} else {
				listReturn.add(data);
			}
		}
		
		
		return listReturn;	
	}
	

	public List selectStoreMed() throws Exception {
		List<StoreMed> list = null;
		
		Ehcache cache = cacheManager.getEhcache("store-med");
		Element element = cache.get("store-med-key");
		
		if(element == null){
			
			//log.info("select - StorePlannerDAO-->" + this.getMedsTemplateDAO());
			
			list = this.getStoreMedDAO().getStoreMedAll();
			
			Element elep = new Element("store-med-key", list);
			cache.put(elep);
			//log.info("store med from db");
		} else {
			list = (List)element.getValue();
			//log.info("store med from cache");
		}
		
		return list;	
	}
	
	//obselete - cache name is same as cache key
	public void removeCache(String cacheKey){
		Ehcache cache = cacheManager.getEhcache(cacheKey);
		Element element = cache.get(cacheKey);
		
		if(element != null){
			cache.remove(cacheKey);
			//cache.flush(cachekey);
		} 
		return;
	}
	
	public CorpItemRog validItemsDivision(List list, Integer cic, String groupsCd, String division_id){
		CorpItemRog item = null;
		CorpItemRog temp = null;
		String groupCd = "";
		
		String primaryRog = "";
		
		if(!"".equalsIgnoreCase(division_id) && !"**".equalsIgnoreCase(division_id)) {
			primaryRog = this.getRogDivisionDAO().getPrimaryRogByDivision(division_id);
		}
		
		if(list.size() > 0) {
			if("**".equalsIgnoreCase(division_id)) { 
				for (int i = 0; i < list.size(); i++){
					temp = (CorpItemRog)list.get(i);
					
					groupCd = temp.getGroupCd().toString();
					
					if(cic.equals(temp.getCorpItemCd()) && (groupsCd.indexOf(groupCd) >=0 )) {
						item = (CorpItemRog)list.get(i);
						break;
					}
				}
				
			} else { //if(!"".equalsIgnoreCase(division_id) && !"**".equalsIgnoreCase(division_id)) {
				for (int i = 0; i < list.size(); i++){
					temp = (CorpItemRog)list.get(i);
					
					groupCd = temp.getGroupCd().toString();
					//System.out.println("cic="+cic+" groupCd="+groupCd+" rog="+temp.getRog()+" division="+temp.getAvtDivisionId());
					if(cic.equals(temp.getCorpItemCd()) && (groupsCd.indexOf(groupCd) >=0 )	
							&& division_id.equalsIgnoreCase(temp.getAvtDivisionId())) {
						
						if(primaryRog.equalsIgnoreCase(temp.getRog())){
							item = (CorpItemRog)list.get(i);
							break;
						}
					}
				}
				
				// if primary rog/item is not found, use anyone available in the division
				if(item == null)
				for (int i = 0; i < list.size(); i++){
					temp = (CorpItemRog)list.get(i);
					
					groupCd = temp.getGroupCd().toString();
					
					if(cic.equals(temp.getCorpItemCd()) && (groupsCd.indexOf(groupCd) >=0 )	
							&& division_id.equalsIgnoreCase(temp.getAvtDivisionId())) {
						
							item = (CorpItemRog)list.get(i);
							break;
					}
				}
			}
		}

		return item;
	}
	
	public List selectStorePlannerLayout(String store_id, String week_id) throws Exception {
		File pdf = null;
		MedsBean bean = null;
		String division_id = "";
		List list = new ArrayList();
		
		try {

			//log.info("storePlannerDAO-->" + this.getStoreMedDAO());
		
			List listData =  this.getStoreMedDAO().getStoreMedData(store_id, week_id);
			StoreMedData data = null;
			
			List<StoreMed> listReplacedData =  this.getStoreMedDAO().getReplacedStoreMed(store_id, week_id);
			//this.log("replaced planner size = "+listReplacedData.size());
			
			for (Iterator itr = listData.iterator(); itr.hasNext(); ) {
				data = (StoreMedData) itr.next(); 
				
				//this.log("test a ="+!"**".equalsIgnoreCase(data.getWeekId()));
				//this.log("test b ="+this.isReplacedPlanner(data, listReplacedData));

			  if(!"**".equalsIgnoreCase(data.getWeekId().substring(0, 2)) ||
					  "**".equalsIgnoreCase(data.getWeekId().substring(0, 2))&& !this.isReplacedPlanner(data, listReplacedData)){
				  
				bean = new MedsBean();
					String storeID = data.getStoreId().trim();
					while(storeID.length() < 4) {
						storeID = "0" + storeID;
					}
				//bean.setStoreID(data.getStoreId());
				bean.setStoreID(storeID);
				bean.setDivisionID(data.getDivisionId());
				bean.setClusterID(data.getClusterId());
				bean.setTemplate_id(data.getTemplateId().intValue());
				
				bean.setPlannerTypeID(0); // no use
				//bean.setPlannerTypeID(data.getTypeId());
				String typeNm = "";
				String typeAbbrNm = "";
				String sizeNm = "";
				String sizeAbbrNm = "";
				if("A".equalsIgnoreCase(data.getClusterId()) || "B".equalsIgnoreCase(data.getClusterId()) || "C".equalsIgnoreCase(data.getClusterId()) 
						|| "D".equalsIgnoreCase(data.getClusterId())  || "H".equalsIgnoreCase(data.getClusterId())){
					typeNm = data.getTypeNm()+data.getClusterId();
					typeAbbrNm = data.getTypeAbbrNm()+data.getClusterId();
					sizeNm = data.getSizeNm();
					sizeAbbrNm = data.getSizeAbbrNm();
				} else { // liquor
					typeNm = data.getTypeNm();
					typeAbbrNm = data.getTypeAbbrNm();
					
					sizeNm = data.getClusterId().substring(0, 7)+" "+data.getClusterId().substring(7, data.getClusterId().length());
					PlannerSize plannerSize = this.getPlannerSizeDAO().selectPlannerSize(sizeNm);
					if(plannerSize != null)
						sizeAbbrNm =  plannerSize.getSizeAbbrNm();

					//System.out.println("typeAbbrNm="+typeAbbrNm+" cluster="+data.getClusterId()+" sizeNm="+sizeNm+" sizeAbbrNm="+sizeAbbrNm);
				}
				bean.setPlannerTypeNM(typeNm);
				bean.setPlannerTypeAbbrNM(typeAbbrNm);
				
				bean.setPlannerDeptID(data.getDeptId());
				bean.setPlannerDeptNM(data.getDeptNm());
				bean.setPlannerDeptAbbrNM(data.getDeptAbbrNm());
		
				bean.setPlannerSizeID(data.getSizeId().intValue()); 
				//bean.setPlannerSizeNM(data.getSizeNm());
				bean.setPlannerSizeNM(sizeNm);
				//bean.setPlannerSizeAbbrNM(data.getSizeAbbrNm());
				bean.setPlannerSizeAbbrNM(sizeAbbrNm);

				division_id = bean.getDivisionID();
				
				String week_no = week_id.substring(4, 6);
				bean.setPlannerFileNM("Div" + division_id + "_Week_" + week_no
						+ "_" + bean.getPlannerDeptAbbrNM() + "_"
						+ bean.getPlannerTypeAbbrNM() + "_"
						+ bean.getPlannerSizeAbbrNM() + ".pdf");
				//log.info(bean.getPlannerFileNM());
				//System.out.println(bean.getPlannerFileNM());
				
				list.add(bean);
			  } // end if
			} // end for
		} catch (Exception e) {
			throw e;
		} 
		return list;
		}
	
}