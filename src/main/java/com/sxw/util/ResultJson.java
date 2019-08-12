package com.sxw.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 200：表示成功
 * 500：表示错误，错误信息在msg字段中
 * 501：bean验证错误，不管多少个错误都以map形式返回
 * 502：拦截器拦截到用户token出错
 * 555：异常抛出信息
 */
public class ResultJson {
    // 定义jackson对象
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // 响应业务状态
    private Integer status;
    // 响应消息
    private String msg;
    // 响应中的数据
    private Object data;
    // 不使用
    private String ok;

    public static ResultJson build(Integer status, String msg, Object data) {
        return new ResultJson(status, msg, data);
    }

    public static ResultJson ok(Object data) {
        return new ResultJson(data);
    }

    public static ResultJson ok() {
        return new ResultJson(null);
    }

    public static ResultJson errorMsg(String msg) {
        return new ResultJson(500, msg, null);
    }

    public static ResultJson errorMap(Object data) {
        return new ResultJson(501, "error", data);
    }

    public static ResultJson errorTokenMsg(String msg) {
        return new ResultJson(502, msg, null);
    }

    public static ResultJson errorException(String msg) {
        return new ResultJson(555, msg, null);
    }

    public ResultJson() {

    }


    public ResultJson(Integer status, String msg, Object data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    public ResultJson(Object data) {
        this.status = 200;
        this.msg = "OK";
        this.data = data;
    }

    public Boolean isOK() {
        return this.status == 200;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    /**
     * *
     * * @Description: 将json结果集转化为LeeJSONResult对象
     * * 需要转换的对象是一个类
     * * @param jsonData
     * * @param clazz
     * * @return
     *
     */
    public static ResultJson formatToPojo(String jsonData, Class<?> clazz) {
        try {
            if (clazz == null) {
                return MAPPER.readValue(jsonData, ResultJson.class);
            }
            JsonNode jsonNode = MAPPER.readTree(jsonData);
            JsonNode data = jsonNode.get("data");
            Object obj = null;
            if (clazz != null) {
                if (data.isObject()) {
                    obj = MAPPER.readValue(data.traverse(), clazz);
                } else if (data.isTextual()) {
                    obj = MAPPER.readValue(data.asText(), clazz);
                }
            }
            return build(jsonNode.get("status").intValue(), jsonNode.get("msg").asText(), obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * *
     * * @Description: 没有object对象的转化
     * * @param json
     * * @return
     *
     */
    public static ResultJson format(String json) {
        try {
            return MAPPER.readValue(json, ResultJson.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * *
     * * @Description: Object是集合转化
     * * 需要转换的对象是一个list
     * * @param jsonData
     * * @param clazz
     * * @return
     * *
     *
     */
    public static ResultJson formatToList(String jsonData, Class<?> clazz) {
        try {
            JsonNode jsonNode = MAPPER.readTree(jsonData);
            JsonNode data = jsonNode.get("data");
            Object obj = null;
            if (data.isArray() && data.size() > 0) {
                obj = MAPPER.readValue(data.traverse(),
                        MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
            }
            return build(jsonNode.get("status").intValue(), jsonNode.get("msg").asText(), obj);
        } catch (Exception e) {
            return null;
        }
    }

    public String getOk() {
        return ok;
    }

    public void setOk(String ok) {
        this.ok = ok;
    }
}

