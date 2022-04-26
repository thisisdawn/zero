package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptors.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class CartController {


    @Autowired
    private CartService cartService;

//    添加购物车
    @GetMapping
    public String saveCart(Long userId,Cart cart){
        this.cartService.saveCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId="+cart.getSkuId()+"&count="+cart.getCount();
    }

//    跳转并展示添加成功页面
    @GetMapping("addCart.html")
    public String queryCart(Cart cart, Model model){
        BigDecimal count = cart.getCount();
        cart =  this.cartService.queryCart(cart.getSkuId());
        cart.setCount(count);
        model.addAttribute("cart",cart);
        return "addCart.html";
    }

//    查询购物车
    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts =  this.cartService.queryCarts();
        model.addAttribute("carts",carts);
        return "cart";
    }

//    查询购物车选中记录
    @GetMapping("user/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCartsByUserId(@PathVariable("userId") Long userId){
        List<Cart> carts =  this.cartService.queryCheckedCartsByUserId(userId);
        return ResponseVo.ok(carts);

    }


    @GetMapping("test1")
    @ResponseBody
    public String test(){
        System.out.println("测试拦截器.."+ LoginInterceptor.getUserInfo());
        return "hello test..";
    }

    @GetMapping("test2")
    @ResponseBody
    public String test2(){
        long now = System.currentTimeMillis();
        System.out.println("controller方法开始执行..........");
       this.cartService.test1();
       this.cartService.test2();
//        stringFuture1.addCallback(result ->{
//            System.out.println("future1正常执行回调"+result);
//        },ex ->{
//            System.out.println("future1执行失败回调"+ex.getMessage());
//        });
//        stringFuture2.addCallback(result ->{
//            System.out.println("future2正常执行回调"+result);
//        },ex ->{
//            System.out.println("future2执行失败回调"+ex.getMessage());
//        });
        System.out.println("controller方法执行结束.........."+(System.currentTimeMillis()-now));
        return "hello 异常处理器";
    }
}
