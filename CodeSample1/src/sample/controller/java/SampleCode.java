package com.****;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jxls.transformer.XLSTransformer;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.*****;

public class PlannerServlet extends HttpServlet implements Servlet {
	private static final long serialVersionUID = 1L;
	
	private static final String TMP_DIR_PATH = Constants.LOCAL_PDF_PATH;
	
	private static Logger log = Logger.getLogger(PlannerServlet.class);
	
	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doPost( request,response);
	}
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		String msg = "";
		String invalidCIC = "";
		String user_id = MyUtils.handleNullString(request.getRemoteUser()).toLowerCase();
		String opt_type = MyUtils.handleNullString((String) request.getParameter("opt_type"));
		//System.out.println("Entry:PlannerServlet - type="+opt_type);	
		
		String planner_name = MyUtils.handleNullString((String) request.getParameter("planner_name"));
		String med_id = MyUtils.handleNullString((String) request.getParameter("med_id"));
		String size_id = "";
		//String size_id = MyUtils.handleNullString((String) request.getParameter("size_id"));
		String weekYear = MyUtils.handleNullString((String) request.getParameter("week_id"));
		
		String weekFrom = MyUtils.handleNullString((String) request.getParameter("week_from"));
		String weekTo = MyUtils.handleNullString((String) request.getParameter("week_to"));
		
		String cluster_id = MyUtils.handleNullString((String) request.getParameter("cluster_id"));
		String division_id = MyUtils.handleNullString((String) request.getParameter("division_id"));

		String clusterDesc = MyUtils.handleNullString((String) request.getParameter("cluster_desc"));
		String comments = MyUtils.handleNullString((String) request.getParameter("comments"));
		String featureDesc = MyUtils.handleNullString((String) request.getParameter("feature_desc"));
		String featurePrice = MyUtils.handleNullString((String) request.getParameter("feature_price"));

		String openStatus = MyUtils.handleNullString((String) request.getParameter("open_close"));
		String med4x2 = MyUtils.handleNullString((String) request.getParameter("med4x2"));
		String copyMed = MyUtils.handleNullString((String) request.getParameter("copyMed"));
		String copy_id = MyUtils.handleNullString((String) request.getParameter("copy_id"));
		
		String photoFlag = MyUtils.handleNullString((String) request.getParameter("photo"));
		
		String med_id_init = med_id;
		
		String clusterId = null;
		String divisionId = null;
		String clusters[] = request.getParameterValues("clusters");
		String divisions[] = request.getParameterValues("divisions");
		
		boolean alert = true;
		DBManager manager = null;
		PlanningManager planningManager = null;
		MFManager mf = null;
		
		Integer folderId = 0;
		String groupsCd = "";
		String groupsCdByWeek = "";

		String subject = "";
		String content = "";

		try {
			ServletContext application = getServletConfig().getServletContext();
			WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(application);
			manager = (DBManager)context.getBean("dBManager");
			planningManager = (PlanningManager)context.getBean("planningManager");
			mf = (MFManager)context.getBean("mFManager");
			
			MedsTemplate medSelected = null;
			TemplateException medOverride = null;
			int side =  0;
			int shelfLeft =  0;
			int shelfCenter =  0;
			int shelfRight =  0;
			int slotLeft =  0;
			int slotCenter =  0;
			int slotRight =  0;
			
			int shelfBack =  0;
			int slotBack =  0;
			
			if (!"".equalsIgnoreCase(med_id) && !"*".equalsIgnoreCase(med_id)){
				medSelected = planningManager.getMedsTemplate(Integer.valueOf(med_id));
				size_id = medSelected.getSizeId().toString();
				
				side =  medSelected.getSideNbr().intValue();
				shelfLeft =  medSelected.getShelfLeftNbr().intValue();
				shelfCenter =  medSelected.getShelfCentreNbr().intValue();
				shelfRight =  medSelected.getShelfRightNbr().intValue();
				slotLeft=  medSelected.getSlotLeftNbr().intValue();
				slotCenter =  medSelected.getSlotCentreNbr().intValue();
				slotRight =  medSelected.getSlotRightNbr().intValue();
				
				if(!"".equalsIgnoreCase(division_id) && !"**".equalsIgnoreCase(division_id)) { // Division specific
					medOverride = planningManager.selectTemplateException(Integer.valueOf(med_id), division_id);
					//log.info("Division specific configuration search");
				}
				if((medOverride== null) && (side == 4)){
					medOverride = planningManager.selectTemplateException(Integer.valueOf(med_id), "**");
					//log.info("4 sides configuration search");
				}
				
				if(medOverride!= null){
					side = medOverride.getSideNbr();
					shelfLeft =  medOverride.getShelfLeftNbr().intValue();
					shelfCenter =  medOverride.getShelfFrontNbr().intValue();
					shelfRight =  medOverride.getShelfRightNbr().intValue();						
					
					slotLeft=  medOverride.getSlotLeftNbr().intValue();
					slotCenter =  medOverride.getSlotFrontNbr().intValue();
					slotRight =  medOverride.getSlotRightNbr().intValue();
					
					if(side == 4){
						shelfBack =  medOverride.getShelfBackNbr().intValue();
						slotBack =  medOverride.getSlotBackNbr().intValue();
					}
				}
				
				folderId = medSelected.getFolderId();
				groupsCd = planningManager.getGroupsCdByfolderId(folderId, "*"); //default
		
				groupsCdByWeek = planningManager.getGroupsCdByfolderId(folderId, weekYear);
				if ((groupsCdByWeek != null) && !"".equalsIgnoreCase(groupsCdByWeek))
					groupsCd = groupsCdByWeek;
			}
			//System.out.println("Entry:PlannerServlet - selection=["+opt_type+"]"+" med_id=["+med_id+"]");
			
					if (index ==1) { // Second call
						//log.info("PlannerServlet: Update - save planner copy.");						
						if("Yes".equalsIgnoreCase(med4x2)){
							med_id = planningManager.get4by2Id(med_id);
							//log.info("PlannerServlet: Update - save planner to 4x2, id ="+med_id);	

						} else { // copyMed
							String old_id = med_id;
							med_id = planningManager.getCopyId(med_id);
							//log.info("PlannerServlet: Update - save planner to another planner, id ["+ old_id +"] to id["+ med_id + "]");	
						}
						if(!"".equalsIgnoreCase(med_id)){
							medSelected = planningManager.getMedsTemplate(Integer.valueOf(med_id)); 
							size_id = medSelected.getSizeId().toString();
							
							side =  medSelected.getSideNbr().intValue();
							shelfLeft =  medSelected.getShelfLeftNbr().intValue();
							shelfCenter =  medSelected.getShelfCentreNbr().intValue();
							shelfRight =  medSelected.getShelfRightNbr().intValue();
							slotLeft=  medSelected.getSlotLeftNbr().intValue();
							slotCenter =  medSelected.getSlotCentreNbr().intValue();
							slotRight =  medSelected.getSlotRightNbr().intValue();
								medOverride = null;
								//log.info("division_id="+division_id);
								if(!"".equalsIgnoreCase(division_id) && !"**".equalsIgnoreCase(division_id)) { // Division specific
									medOverride = planningManager.selectTemplateException(Integer.valueOf(med_id), division_id);
									//log.info("Division specific configuration search.");
								}
								if((medOverride== null) && (side == 4)){
									medOverride = planningManager.selectTemplateException(Integer.valueOf(med_id), "**");
									//log.info("4 sides configuration search");
								}
								
								if(medOverride!= null){
									side = medOverride.getSideNbr();
									shelfLeft =  medOverride.getShelfLeftNbr().intValue();
									shelfCenter =  medOverride.getShelfFrontNbr().intValue();
									shelfRight =  medOverride.getShelfRightNbr().intValue();						
									
									slotLeft=  medOverride.getSlotLeftNbr().intValue();
									slotCenter =  medOverride.getSlotFrontNbr().intValue();
									slotRight =  medOverride.getSlotRightNbr().intValue();
									
									if(side == 4){
										shelfBack =  medOverride.getShelfBackNbr().intValue();
										slotBack =  medOverride.getSlotBackNbr().intValue();
									}
								}
							
							folderId = medSelected.getFolderId();
							groupsCd = planningManager.getGroupsCdByfolderId(folderId, "*");
							
							groupsCdByWeek = planningManager.getGroupsCdByfolderId(folderId, weekYear);
							if ((groupsCdByWeek != null) && !"".equalsIgnoreCase(groupsCdByWeek))
								groupsCd = groupsCdByWeek;
						}
						
					}
					
					clusterId = cluster_id;
					divisionId = division_id;
					String statusCd = "Y";
					String manualCd = "N";
					
					Integer templateId = Integer.valueOf(med_id);
					Integer plannerSizeId = Integer.valueOf(size_id);
					Planner planner = planningManager.getPlanner(Integer.valueOf(med_id), cluster_id, Integer.valueOf(size_id), division_id, weekYear);
					Planner originalPlanner = planner;
					
					PlannerPhoto plannerPhoto = planningManager.selectPlannerPhoto(Integer.valueOf(med_id), cluster_id, division_id, "*");
					

					Planner newPlanner = new Planner();
					newPlanner.setTemplateId(templateId);
					newPlanner.setPlannerSizeId(plannerSizeId);
					newPlanner.setWeekYearId(weekYear);
					newPlanner.setClusterDsc(clusterDesc);
					newPlanner.setCommentsTxt(comments);
					newPlanner.setFeatureDsc(featureDesc);
					newPlanner.setFeaturePrc(featurePrice);
					//newPlanner.setStatusCd(statusCd);
					//newPlanner.setManualCd(manualCd);
					
					PlannerPhoto newPlannerPhoto = new PlannerPhoto();
					newPlannerPhoto.setTemplateId(Integer.valueOf(med_id));
					newPlannerPhoto.setClusterId(cluster_id);
					newPlannerPhoto.setAvtDivisionId(division_id);
					newPlannerPhoto.setActive(photoFlag);
					newPlannerPhoto.setWeekYearId("*");// to be updated
					byte[]photoFile = null;
					String fileName = UtilManager.photoFileName(med_id, cluster_id, division_id, "*"); // no week dependence for now
					File file = new File(TMP_DIR_PATH+fileName);
					//log.info("Photo file name ="+fileName);
					boolean photoExists = file.exists();
					if (photoExists) {
						//log.info("Photo file exists!");
						photoFile = UtilManager.readImage(fileName, TMP_DIR_PATH);
						newPlannerPhoto.setPhotoPic(photoFile);
						//file.delete();
					}
					
					
}