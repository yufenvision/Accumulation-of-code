package com.yuf.crm.service.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yuf.crm.domain.Resource;
import com.yuf.crm.mapper.ResourceMapper;
import com.yuf.crm.query.ResourceQuery;
import com.yuf.crm.service.IResourceService;
import com.yuf.crm.utils.PageResult;

@Service
public class ResourceServiceImpl implements IResourceService{

	private ResourceMapper resourceMapper;
	
	@Autowired
	public void setResourceMapper(ResourceMapper resourceMapper){
		this.resourceMapper = resourceMapper;
		
		resourceMapper.createTable();
	}
	
	private String[] DEFAULT_MVC_PACKGES = {"com.yuf.crm.web.controller"};
	
	private Map<String,List<String>> modules = new HashMap<>();
	
	@Override
	public void initControllers() {
		// 初始化
		for (String pkgPath : DEFAULT_MVC_PACKGES) {
			try {
				loadPkgController(pkgPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	
	/**
	 * 加载包下资源
	 * @param pkgPath
	 * @throws IOException
	 */
	private void loadPkgController(String pkgPath) throws IOException{
		if(modules.get(pkgPath)==null){
			List<String> clzNames = new ArrayList<>();
		
			/*
			 * 把包下的模块缓存
			 */
			String packageDirName = pkgPath.replace(".","/");
			
			Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
			
			while(dirs.hasMoreElements()){
				URL url = dirs.nextElement();
				File file = new File(url.getFile());
				//把此目录下的所有文件列出
				String[] classes = file.list();
				//循环此数组，并把.class去掉
				for (String className : classes) {
					className = className.substring(0,className.length()-6);
					//拼接上包名，变成全限定名
					String qName = pkgPath+"."+className;
					
					try {
						//检查是否需要展示的资源（进而是否支持权限控制）
						if(Class.forName(qName).isAnnotationPresent(com.yuf.crm.utils.Resource.class)){
							//找到包下所有的类
							clzNames.add(qName);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			modules.put(pkgPath, clzNames);
		}
	}
	
	
	
	
	@Override
	public Map<String, List<String>> getControllers() {
		return modules;
	}

	@Override
	public void importControllerResources(String module) {
		try {
			//获得模块控制器对应的字节码对象
			Class<?> clz = Class.forName(module);
			
			//获得控制器方法
			com.yuf.crm.utils.Resource cResource = (com.yuf.crm.utils.Resource) clz.getAnnotation(com.yuf.crm.utils.Resource.class); 
			//如果有自定义（中文）名称，就用定义的，没有就用简单类名
			String cName = !cResource.value().equals("")?cResource.value():clz.getSimpleName();
			//扫描方法
			Method[] declareMethods = clz.getMethods();
			for (Method method : declareMethods) {
				//验证打了资源标签的方法
				if(method.isAnnotationPresent(com.yuf.crm.utils.Resource.class)){
					//获得方法名称
					com.yuf.crm.utils.Resource mResource = method.getAnnotation(com.yuf.crm.utils.Resource.class);
					String mName = !mResource.value().equals("")?mResource.value():method.getName();
					addModuleResource(module, cName,method.getName(),mName);
				};
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	
	private void addModuleResource(String module,String moduleAlias,String methodName,String methodNameAlias){
		//获得资源地址
		String url = module+":"+methodName;
		//获得资源名称
		String name= moduleAlias+":"+methodNameAlias;
		
		if(!checkExistResource(url)){
			Resource r = new Resource();
			r.setName(name);
			r.setUrl(url);
			r.setController(module);
			
			resourceMapper.save(r);
		}
		
	}
	
	//检查数据库中是否已有该资源
	private boolean checkExistResource(String url){
		List<Resource> list = resourceMapper.getByUrl(url);// baseDao.findByHql("FROM Resource WHERE name=?", qName);
		if(list.size()>0){
			return true;
		}
		return false;
	}
	
	
	@Override
	public PageResult<Resource> findPageResult(ResourceQuery query) {
		PageResult<Resource> pr = new PageResult<Resource>();
		Long count = resourceMapper.getCount(query);
		pr.setTotal(count);
		List<Resource> list = resourceMapper.queryList(query);
		pr.setRows(list);
		return pr;
	}


	@Override
	public void del(Long id) {
		if(id != null){
			resourceMapper.delete(id);
		}
	}

}
