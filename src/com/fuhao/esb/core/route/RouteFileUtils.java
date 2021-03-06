package com.fuhao.esb.core.route;
import com.fuhao.esb.common.vo.Constants;
import com.fuhao.esb.common.vo.Constants.ROUTE_POLICY;
import com.fuhao.esb.core.component.utils.ESBFileUtils;
import com.fuhao.esb.core.component.utils.ESBLogUtils;
import com.fuhao.esb.core.component.utils.ObjectUtils;
import com.fuhao.esb.core.exception.ESBBaseCheckedException;
import com.fuhao.esb.core.route.protocal.IProtocalInfo;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import weblogic.wsee.util.ObjectUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * package name is  com.fuhao.esb.core.route
 * Created by fuhao on 13-12-26.
 * Project Name esb-java
 * 路由配置文件读取工具类
 */
public class RouteFileUtils {
    private  static  ESBLogUtils logger = ESBLogUtils.getLogger(RouteFileUtils.class);



    public static Long getRouteConfVersion(){
        String version = readRouteConfFile().attributeValue("version") ;
         return Long.parseLong(version);
    }

    public static void setRouteConfVersion(){
        RouteCache.getInstance().setRouteVersion(getRouteConfVersion());
    }

    public  static  Long getRefreshCapacity(){
        String version = readRouteConfFile().attributeValue("refreshCapacity") ;
        return Long.parseLong(version);
    }

    /**
     * 读取解析路由配置文件
     * @return
     */
    public static  Element readRouteConfFile(){
        SAXReader reader = new SAXReader();
        Document doc = null;
        Element   rootElement = null;
        try {
            doc = reader.read(new File(ESBFileUtils.getESBRootPath() + "/router.xml"));
            rootElement = doc.getRootElement();

        } catch (Exception ex) {
            ESBBaseCheckedException e = new ESBBaseCheckedException("路由配置读取失败", ex);
            logger.error(e);
        }
        return rootElement;
    }


    /**
     * 加载路由规则，路由配置，协议信息
     * @throws ESBBaseCheckedException
     */
    public  static void loadRoutConfig() throws ESBBaseCheckedException {

        List<Element> routsRule = null;
        List<Element> routs = null;
        List<Element> protocals = null;
        // 设置路由版本号
        setRouteConfVersion();
        Element   rootElement =  readRouteConfFile() ;


        // 组件配置信息
        routsRule = rootElement.element("routePolicyInfos").elements();
        routs = rootElement.element("routes").elements();
        protocals = rootElement.element("protocals").elements();

        logger.info("开始初始化已经注册的" + routs.size() + "个路由");


        /**
         * 获取对应配置文件中的路由信息，cache顺序，先协议，后路由，在规则，因为后面书写是依赖前者的。
         */
        // 协议处理器
        Map<String, IProtocalInfo> protocalConfigParameters = getprotocals(protocals);
        // 路由处理器
        Map<String, RouteProtocalInfo> routConfigParameters = getRouts(routs);
        // 加载路由规则处理器:
        Map<String, RoutePolicyInfo> ruleConfigParameters = getRoutesRule(routsRule);

        /**
         * 和内存中数据对比，并更新。
         */
        RouteCache.getInstance().cacheMapProtocalConf(protocalConfigParameters);
        RouteCache.getInstance().cacheMapRouteConf(routConfigParameters);
        RouteCache.getInstance().cacheMapRouteRule(ruleConfigParameters);

        // update  RouteCache.getInstance().getMapRouteCacheConf()


    }




    public  static  Map<String, RoutePolicyInfo>   getRoutesRule(List<Element> routsRule)  throws ESBBaseCheckedException{
        // 从配置文件加载组件
        Map<String, RoutePolicyInfo> ruleConfigParameters = new ConcurrentHashMap<String,RoutePolicyInfo>();
        for (Element rule : routsRule) {
            String id = rule.attributeValue("routeRuleID");
            // 读取结点的属性信息
            Map eleMap =  ObjectUtils.getParamter(rule);
            try{
                RoutePolicyInfo routePolicyInfo = (RoutePolicyInfo) ObjectUtils.map2vo(eleMap, new RoutePolicyInfo());
                // 生成配置信息对象
                ruleConfigParameters.put(id, routePolicyInfo);
            }catch (Exception ex){
                throw new ESBBaseCheckedException(ex);
            }

        }
        return ruleConfigParameters;
    }
    public  static   Map<String, RouteProtocalInfo>   getRouts(List<Element> routs) throws ESBBaseCheckedException{

        // 从配置文件加载组件
        Map<String, RouteProtocalInfo> routConfigParameters = new ConcurrentHashMap<String,RouteProtocalInfo>();
        for (Element rout : routs) {
            String id = rout.attributeValue("routeProtocalInfoID");
            // 读取结点的属性信息
            Map eleMap =  ObjectUtils.getParamter(rout);
            try{
                RouteProtocalInfo routeProtocalInfo = (RouteProtocalInfo) ObjectUtils.map2vo(eleMap, new RouteProtocalInfo());
                // 生成配置信息对象
                routConfigParameters.put(id, routeProtocalInfo);
            }catch (Exception ex){
                throw new ESBBaseCheckedException(ex);
            }

        }
       return routConfigParameters;
    }
    public  static  Map<String, IProtocalInfo>   getprotocals(List<Element> protocals) throws ESBBaseCheckedException{

        // 从配置文件加载组件
        Map<String, IProtocalInfo> protocalConfigParameters = new ConcurrentHashMap<String,IProtocalInfo>();
        for (Element protocal : protocals) {
            String id = protocal.attributeValue("routeRuleID");
            String protocalType = protocal.attributeValue("protocalType") ;
            // 读取结点的属性信息
            Map eleMap =  ObjectUtils.getParamter(protocal);

            try{
                IProtocalInfo protocalInfo = (IProtocalInfo) ObjectUtils.map2vo(eleMap, Constants.getProtocalInfoType(protocalType));
                // 生成配置信息对象
                protocalConfigParameters.put(id, protocalInfo);
            }catch (Exception ex){
                throw new ESBBaseCheckedException(ex);
            }

        }
       return protocalConfigParameters;
    }


}
