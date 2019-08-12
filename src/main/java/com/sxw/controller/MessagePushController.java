package com.sxw.controller;

import com.sxw.common.quartz.QuartzService;
import com.sxw.util.ResultJson;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/push")
public class MessagePushController {

	static Logger logger = LoggerFactory.getLogger(MessagePushController.class);

	@Autowired private QuartzService quartzService;


	@ApiOperation(value = "增加用户订阅")
	@ApiImplicitParam(name = "xxx", value = "xxx", required = true, dataType = "int",paramType = "path")
	@GetMapping(value = "/xxx",produces = "application/json; charset=UTF-8")
	public ResultJson add(@PathVariable("xxx") int xxx) {
		if (StringUtils.isEmpty(xxx)) {
			return ResultJson.errorMsg("Param 'xxx' cann't be empty!");
		}

		return ResultJson.ok();
	}

}
