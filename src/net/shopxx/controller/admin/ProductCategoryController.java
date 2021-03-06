/*
 * Copyright 2005-2013 shopxx.net. All rights reserved.
 * Support: http://www.shopxx.net
 * License: http://www.shopxx.net/license
 */
package net.shopxx.controller.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import net.shopxx.Message;
import net.shopxx.entity.Brand;
import net.shopxx.entity.Product;
import net.shopxx.entity.ProductCategory;
import net.shopxx.entity.Store;
import net.shopxx.service.BrandService;
import net.shopxx.service.ProductCategoryService;
import net.shopxx.util.WebUtils;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller - 商品分类
 * 
 * @author SHOP++ Team
 * @version 3.0
 */
@Controller("adminProductCategoryController")
@RequestMapping("/admin/product_category")
public class ProductCategoryController extends BaseController {
    
	@Resource(name = "productCategoryServiceImpl")
	private ProductCategoryService productCategoryService;
	@Resource(name = "brandServiceImpl")
	private BrandService brandService;

	/**
	 * 添加
	 */
	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public String add(ModelMap model) {
//		model.addAttribute("productCategoryTree", productCategoryService.findTree());
		/**
		 * 店铺管理员，查询该店铺及商城的分类
		 * 非店铺管理员，查询商城分类
		 */
		model.addAttribute("store", WebUtils.getStore());
		model.addAttribute("productCategoryTree", productCategoryService.findTreeForStore());
		model.addAttribute("brands", brandService.findAll());
		return "/admin/product_category/add";
	}

	/**
	 * 保存
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String save(ProductCategory productCategory, Long parentId, Long[] brandIds, RedirectAttributes redirectAttributes) {
		productCategory.setParent(productCategoryService.find(parentId));
		productCategory.setBrands(new HashSet<Brand>(brandService.findList(brandIds)));
		if (!isValid(productCategory)) {
			return ERROR_VIEW;
		}
		productCategory.setTreePath(null);
		productCategory.setGrade(null);
		productCategory.setChildren(null);
		productCategory.setProducts(null);
		productCategory.setParameterGroups(null);
		productCategory.setAttributes(null);
		productCategory.setPromotions(null); 
		productCategory.setEntcode(WebUtils.getXentcode());
		productCategory.setStore(WebUtils.getStore());//设置所属店铺 cgd 2014-10-15
		productCategoryService.save(productCategory);
		addFlashMessage(redirectAttributes, SUCCESS_MESSAGE);
		return "redirect:list.jhtml";
	}

	/**
	 * 编辑
	 */
	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public String edit(Long id, ModelMap model) {
		ProductCategory productCategory = productCategoryService.find(id);
		Store store = WebUtils.getStore();
		if (store == null) { //非店铺管理员，查询所有分类
			model.addAttribute("productCategoryTree", productCategoryService
					.findTree());
		} else { //店铺管理员，查询该店铺的分类及商城分类
			model.addAttribute("productCategoryTree", productCategoryService
					.findTreeForStore());
		}
		model.addAttribute("store", store);
		model.addAttribute("brands", brandService.findAll());
		model.addAttribute("productCategory", productCategory);
		model.addAttribute("children", productCategoryService.findChildren(productCategory));
		return "/admin/product_category/edit";
	}

	/**
	 * 更新
	 */
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public String update(ProductCategory productCategory, Long parentId, Long[] brandIds, RedirectAttributes redirectAttributes) {
		productCategory.setParent(productCategoryService.find(parentId));
		productCategory.setBrands(new HashSet<Brand>(brandService.findList(brandIds)));
		if (!isValid(productCategory)) {
			return ERROR_VIEW;
		}
		if (productCategory.getParent() != null) {
			ProductCategory parent = productCategory.getParent();
			if (parent.equals(productCategory)) {
				return ERROR_VIEW;
			}
			List<ProductCategory> children = productCategoryService.findChildren(parent);
			if (children != null && children.contains(parent)) {
				return ERROR_VIEW;
			}
		}
		productCategory.setEntcode(WebUtils.getXentcode());
		productCategory.setStore(productCategoryService.find(productCategory.getId()).getStore());//设置所属店铺 cgd 2014-10-15
		productCategoryService.update(productCategory, "treePath", "grade", "children", "products", "parameterGroups", "attributes", "promotions");
		addFlashMessage(redirectAttributes, SUCCESS_MESSAGE);
		return "redirect:list.jhtml";
	}

	/**
	 * 列表
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String list(ModelMap model) {
		model.addAttribute("productCategoryTree", productCategoryService.findTree());
		return "/admin/product_category/list";
	}

	/**
	 * 删除
	 */
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	public @ResponseBody
	Message delete(Long id) {
		ProductCategory productCategory = productCategoryService.find(id);
		if (productCategory == null) {
			return ERROR_MESSAGE;
		}
		Set<ProductCategory> children = productCategory.getChildren();
		if (children != null && !children.isEmpty()) {
			return Message.error("admin.productCategory.deleteExistChildrenNotAllowed");
		}
		Set<Product> products = productCategory.getProducts();
		if (products != null && !products.isEmpty()) {
			return Message.error("admin.productCategory.deleteExistProductNotAllowed");
		}
		productCategoryService.delete(id);
		return SUCCESS_MESSAGE;
	}

}