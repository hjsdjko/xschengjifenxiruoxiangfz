
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 试卷表
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/exampaper")
public class ExampaperController {
    private static final Logger logger = LoggerFactory.getLogger(ExampaperController.class);

    private static final String TABLE_NAME = "exampaper";

    @Autowired
    private ExampaperService exampaperService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private CaozuorizhiService caozuorizhiService;//操作日志
    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private ExampapertopicService exampapertopicService;//试卷选题
    @Autowired
    private ExamquestionService examquestionService;//试题表
    @Autowired
    private ExamrecordService examrecordService;//考试记录表
    @Autowired
    private ExamredetailsService examredetailsService;//答题详情表
    @Autowired
    private ExamrewrongquestionService examrewrongquestionService;//错题表
    @Autowired
    private ForumService forumService;//论坛
    @Autowired
    private JiaoshiService jiaoshiService;//教师
    @Autowired
    private LiuyanService liuyanService;//留言板
    @Autowired
    private NewsService newsService;//公告信息
    @Autowired
    private YonghuService yonghuService;//学生
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("学生".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("教师".equals(role))
            params.put("jiaoshiId",request.getSession().getAttribute("userId"));
        params.put("exampaperDeleteStart",1);params.put("exampaperDeleteEnd",1);
        CommonUtil.checkMap(params);
        PageUtils page = exampaperService.queryPage(params);

        //字典表数据转换
        List<ExampaperView> list =(List<ExampaperView>)page.getList();
        for(ExampaperView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"列表查询",list.toString());
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ExampaperEntity exampaper = exampaperService.selectById(id);
        if(exampaper !=null){
            //entity转view
            ExampaperView view = new ExampaperView();
            BeanUtils.copyProperties( exampaper , view );//把实体数据重构到view中
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
    caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"单条数据查看",view.toString());
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody ExampaperEntity exampaper, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,exampaper:{}",this.getClass().getName(),exampaper.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");

        Wrapper<ExampaperEntity> queryWrapper = new EntityWrapper<ExampaperEntity>()
            .eq("exampaper_name", exampaper.getExampaperName())
            .eq("exampaper_date", exampaper.getExampaperDate())
            .eq("exampaper_myscore", exampaper.getExampaperMyscore())
            .eq("kemu_types", exampaper.getKemuTypes())
            .eq("exampaper_types", exampaper.getExampaperTypes())
            .eq("zujuan_types", exampaper.getZujuanTypes())
            .eq("exampaper_delete", 1)
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ExampaperEntity exampaperEntity = exampaperService.selectOne(queryWrapper);
        if(exampaperEntity==null){
            exampaper.setExampaperDelete(1);
            exampaper.setCreateTime(new Date());
            exampaperService.insert(exampaper);
            caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"新增",exampaper.toString());
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody ExampaperEntity exampaper, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,exampaper:{}",this.getClass().getName(),exampaper.toString());
        ExampaperEntity oldExampaperEntity = exampaperService.selectById(exampaper.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");

            exampaperService.updateById(exampaper);//根据id更新
            List<String> strings = caozuorizhiService.clazzDiff(exampaper, oldExampaperEntity, request,new String[]{"updateTime"});
            caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"修改",strings.toString());
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<ExampaperEntity> oldExampaperList =exampaperService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        ArrayList<ExampaperEntity> list = new ArrayList<>();
        for(Integer id:ids){
            ExampaperEntity exampaperEntity = new ExampaperEntity();
            exampaperEntity.setId(id);
            exampaperEntity.setExampaperDelete(2);
            list.add(exampaperEntity);
        }
        if(list != null && list.size() >0){
            exampaperService.updateBatchById(list);
        }

        caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"删除",oldExampaperList.toString());
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //.eq("time", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
        try {
            List<ExampaperEntity> exampaperList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            ExampaperEntity exampaperEntity = new ExampaperEntity();
//                            exampaperEntity.setExampaperName(data.get(0));                    //试卷名称 要改的
//                            exampaperEntity.setExampaperDate(Integer.valueOf(data.get(0)));   //考试时长(分钟) 要改的
//                            exampaperEntity.setExampaperMyscore(Integer.valueOf(data.get(0)));   //试卷总分数 要改的
//                            exampaperEntity.setKemuTypes(Integer.valueOf(data.get(0)));   //科目 要改的
//                            exampaperEntity.setExampaperTypes(Integer.valueOf(data.get(0)));   //试卷状态 要改的
//                            exampaperEntity.setZujuanTypes(Integer.valueOf(data.get(0)));   //组卷方式 要改的
//                            exampaperEntity.setExampaperDelete(1);//逻辑删除字段
//                            exampaperEntity.setCreateTime(date);//时间
                            exampaperList.add(exampaperEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        exampaperService.insertBatch(exampaperList);
                        caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"批量新增",exampaperList.toString());
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = exampaperService.queryPage(params);

        //字典表数据转换
        List<ExampaperView> list =(List<ExampaperView>)page.getList();
        for(ExampaperView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"列表查询",list.toString());
        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ExampaperEntity exampaper = exampaperService.selectById(id);
            if(exampaper !=null){


                //entity转view
                ExampaperView view = new ExampaperView();
                BeanUtils.copyProperties( exampaper , view );//把实体数据重构到view中

                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                    caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"单条数据查看",view.toString());
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody ExampaperEntity exampaper, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,exampaper:{}",this.getClass().getName(),exampaper.toString());
        Wrapper<ExampaperEntity> queryWrapper = new EntityWrapper<ExampaperEntity>()
            .eq("exampaper_name", exampaper.getExampaperName())
            .eq("exampaper_date", exampaper.getExampaperDate())
            .eq("exampaper_myscore", exampaper.getExampaperMyscore())
            .eq("kemu_types", exampaper.getKemuTypes())
            .eq("exampaper_types", exampaper.getExampaperTypes())
            .eq("zujuan_types", exampaper.getZujuanTypes())
            .eq("exampaper_delete", exampaper.getExampaperDelete())
//            .notIn("exampaper_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ExampaperEntity exampaperEntity = exampaperService.selectOne(queryWrapper);
        if(exampaperEntity==null){
            exampaper.setExampaperDelete(1);
            exampaper.setCreateTime(new Date());
        exampaperService.insert(exampaper);

            caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"前台新增",exampaper.toString());
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

