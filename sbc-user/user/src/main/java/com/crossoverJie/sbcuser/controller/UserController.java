package com.crossoverJie.sbcuser.controller;

import com.alibaba.fastjson.JSON;
import com.crossoverJie.order.feign.api.OrderServiceClient;
import com.crossoverJie.order.vo.req.OrderNoReqVO;
import com.crossoverJie.order.vo.res.OrderNoResVO;
import com.crossoverJie.sbcorder.common.enums.StatusEnum;
import com.crossoverJie.sbcorder.common.res.BaseResponse;
import com.crossoverJie.sbcuser.req.OrderNoReq;
import com.crossoverJie.sbcuser.res.UserRes;
import com.crossoverJie.user.api.UserService;
import com.crossoverJie.user.vo.req.UserReqVO;
import com.crossoverJie.user.vo.res.UserResVO;
import com.google.common.util.concurrent.RateLimiter;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;

/**
 * Function:user控制器
 *
 * @author crossoverJie
 *         Date: 2017/6/7 下午11:55
 * @since JDK 1.8
 */
@RestController
@Api(value = "userApi", description = "用户API", tags = {"用户服务"})
public class UserController implements UserService {
    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrderServiceClient orderServiceClient;

    private final static int COUNT = 10;

    @Resource(name = "concurrentTestThread")
    private ExecutorService executorService;


    @Override
    public BaseResponse<UserResVO> getUser(@RequestBody UserReqVO userReq) {
        OrderNoReq req = new OrderNoReq();
        req.setAppId("1");
        req.setReqNo("1213");

        //调用远程服务
//        ResponseEntity<Object> res = restTemplate.postForEntity("http://localhost:8181/orderService/getOrderNo", req, Object.class);
//        logger.info("res=" + JSON.toJSONString(res));




        logger.debug("入参=" + JSON.toJSONString(userReq));
        UserRes userRes = new UserRes();
        userRes.setUserId(123);
        userRes.setUserName("张三");

        userRes.setReqNo(userReq.getReqNo());
        userRes.setCode(StatusEnum.SUCCESS.getCode());
        userRes.setMessage("成功");

        return userRes;
    }

    @Override
    public BaseResponse<UserResVO> getUserByFeign(@RequestBody UserReqVO userReq) {
        //调用远程服务
        OrderNoReqVO vo = new OrderNoReqVO();
        vo.setAppId(1L);
        vo.setReqNo(userReq.getReqNo());

        for (int i = 0; i < 10; i++) {
            executorService.execute(new Worker(vo, orderServiceClient));
        }

        UserRes userRes = new UserRes();
        userRes.setUserId(123);
        userRes.setUserName("张三");

        userRes.setReqNo(userReq.getReqNo());
        userRes.setCode(StatusEnum.SUCCESS.getCode());
        userRes.setMessage("成功");

        return userRes;
    }

    @Override
    public BaseResponse<UserResVO> getUserByFeignBatch(@RequestBody UserReqVO userReqVO) {
        //调用远程服务
        OrderNoReqVO vo = new OrderNoReqVO();
        vo.setReqNo(userReqVO.getReqNo());
        vo.setAppId(1L);

        RateLimiter limiter = RateLimiter.create(2.0);
        //批量调用
        for (int i = 0; i < COUNT; i++) {
            double acquire = limiter.acquire();
            logger.debug("获取令牌成功!,消耗=" + acquire);
            BaseResponse<OrderNoResVO> orderNo = orderServiceClient.getOrderNo(vo);
            logger.debug("远程返回:" + JSON.toJSONString(orderNo));
        }

        UserRes userRes = new UserRes();
        userRes.setUserId(123);
        userRes.setUserName("张三");

        userRes.setReqNo(userReqVO.getReqNo());
        userRes.setCode(StatusEnum.SUCCESS.getCode());
        userRes.setMessage("成功");

        return userRes;
    }


    @Override
    public BaseResponse<OrderNoResVO> getUserByHystrix(@RequestBody UserReqVO userReqVO) {

        OrderNoReqVO vo = new OrderNoReqVO();
        vo.setAppId(123L);
        vo.setReqNo(userReqVO.getReqNo());
        BaseResponse<OrderNoResVO> orderNo = orderServiceClient.getOrderNo(vo);
        return orderNo;
    }


    private static class Worker implements Runnable {

        private OrderNoReqVO vo;
        private OrderServiceClient orderServiceClient;

        public Worker(OrderNoReqVO vo, OrderServiceClient orderServiceClient) {
            this.vo = vo;
            this.orderServiceClient = orderServiceClient;
        }

        @Override
        public void run() {

            BaseResponse<OrderNoResVO> orderNo = orderServiceClient.getOrderNoCommonLimit(vo);
            logger.info("远程返回:" + JSON.toJSONString(orderNo));

        }
    }

}
