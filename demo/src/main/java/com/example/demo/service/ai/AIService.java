package com.example.demo.service.ai;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.repository.house.entity.House;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.Border;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI服务 - 对接大模型API
 */
@Slf4j
@Service
public class AIService {

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.url:https://attachment.cdn/img/22897debd75f.png}")
    private String apiUrl;

    @Value("${ai.model:qwen-turbo}")
    private String model;

    @Value("${ai.provider:qwen}")
    private String provider;

    private final OkHttpClient client;
    
    @Autowired
    private HouseMapper houseMapper;


    public AIService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * AI通用调用入口：根据不同模型提供商组装请求并返回生成结果。
     */
    public String generateText(String systemPrompt, String userPrompt) {
        try {
            // 根据不同的AI提供商构建不同的请求格式
            if ("qwen".equalsIgnoreCase(provider)) {
                return generateTextQwen(systemPrompt, userPrompt);
            } else {
                return generateTextOpenAI(systemPrompt, userPrompt);
            }
        } catch (Exception e) {
            log.error("AI API调用异常", e);
            return null;
        }
    }

    /**
     * 通义千问API调用
     */
    private String generateTextQwen(String systemPrompt, String userPrompt) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        
        // 通义千问的input格式
        JSONObject input = new JSONObject();
        JSONArray messages = new JSONArray();
        
        // 系统提示词
        if (StrUtil.isNotEmpty(systemPrompt)) {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }
        
        // 用户输入
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        
        input.put("messages", messages);
        requestBody.put("input", input);
        
        // 参数设置
        JSONObject parameters = new JSONObject();
        parameters.put("result_format", "message");
        requestBody.put("parameters", parameters);

        RequestBody body = RequestBody.create(
                requestBody.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("通义千问API调用失败: {} - {}", response.code(), errorBody);
                return null;
            }

            String responseBody = response.body().string();
            log.info("通义千问API响应: {}", responseBody);
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            
            // 通义千问的响应格式
            JSONObject output = jsonResponse.getJSONObject("output");
            if (output != null) {
                JSONArray choices = output.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    return message.getString("content");
                }
            }
        }
        return null;
    }

    /**
     * OpenAI格式API调用（兼容其他模型）
     */
    private String generateTextOpenAI(String systemPrompt, String userPrompt) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        
        JSONArray messages = new JSONArray();
        
        // 系统提示词
        if (StrUtil.isNotEmpty(systemPrompt)) {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }
        
        // 用户输入
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);

        RequestBody body = RequestBody.create(
                requestBody.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("AI API调用失败: {} - {}", response.code(), errorBody);
                return null;
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.getJSONObject("message");
                return message.getString("content");
            }
        }
        return null;
    }

    /**
     * AI智能发布核心之一：根据房源基础信息自动生成更吸引人的房源描述。
     */
    public String generateHouseDescription(String title, String houseType, Double area, 
                                           String orientation, String address, 
                                           String facilities, Double rentPrice) {
        String systemPrompt = "你是一个专业的房产文案撰写专家，擅长撰写吸引人的房源描述。" +
                "要求：1. 突出房源亮点 2. 语言生动有感染力 3. 控制在200字以内 4. 真实客观";

        String userPrompt = String.format(
                "请为以下房源生成一段吸引人的描述：\n" +
                "标题：%s\n" +
                "户型：%s\n" +
                "面积：%.1f平米\n" +
                "朝向：%s\n" +
                "位置：%s\n" +
                "配套设施：%s\n" +
                "月租金：%.0f元",
                title, houseType, area, orientation, address, facilities, rentPrice
        );

        return generateText(systemPrompt, userPrompt);
    }

    /**
     * AI智能发布核心之一：优化房源标题，让标题更简洁、更突出卖点。
     */
    public String optimizeHouseTitle(String originalTitle, String houseType, 
                                     String location, String highlights) {
        String systemPrompt = "你是一个房产标题优化专家，擅长创作吸引眼球的房源标题。" +
                "要求：1. 简洁有力 2. 突出核心卖点 3. 控制在30字以内 4. 包含关键信息";

        String userPrompt = String.format(
                "请优化以下房源标题：\n" +
                "原标题：%s\n" +
                "户型：%s\n" +
                "位置：%s\n" +
                "亮点：%s",
                originalTitle, houseType, location, highlights
        );

        return generateText(systemPrompt, userPrompt);
    }

    /**
     * 智能客服回答
     */
    public String chatAnswer(String question, String context) {
        String systemPrompt = "你是一个专业的房屋租赁平台客服助手，名字叫'小租'。" +
                "你的职责是：\n" +
                "1. 解答用户关于租房的各种问题\n" +
                "2. 提供专业、友好、耐心的服务\n" +
                "3. 如果不确定答案，建议用户联系人工客服\n" +
                "4. 回答要简洁明了，一般控制在150字以内\n" +
                "5. 使用礼貌、亲切的语气";

        String userPrompt = question;
        if (StrUtil.isNotEmpty(context)) {
            userPrompt = "上下文信息：" + context + "\n\n用户问题：" + question;
        }

        return generateText(systemPrompt, userPrompt);
    }

    /**
     * 智能推荐理由生成
     */
    public String generateRecommendReason(String userPreference, String houseInfo) {
        String systemPrompt = "你是一个房源推荐专家，擅长分析用户需求并给出推荐理由。" +
                "要求：1. 分析用户偏好 2. 匹配房源特点 3. 给出3-5条推荐理由 4. 每条理由简洁有力";

        String userPrompt = String.format(
                "用户偏好：%s\n\n" +
                "房源信息：%s\n\n" +
                "请分析为什么推荐这套房源给用户，给出具体理由。",
                userPreference, houseInfo
        );

        return generateText(systemPrompt, userPrompt);
    }

    /**
     * AI智能发布核心之一：把自然语言房源描述解析成结构化字段，便于快速录入房源。
     */
    public String parseHouseDescription(String description) {
        String systemPrompt = "你是一个房源信息提取专家，擅长从自然语言描述中提取结构化的房源信息。" +
                "要求：" +
                "1. 直接返回JSON格式的数据，不要任何额外说明" +
                "2. 严格按照指定的JSON格式返回" +
                "3. 如果某个字段无法从描述中提取，使用null" +
                "4. 价格、面积等数字字段只返回数字，不要单位" +
                "5. 地址要尽可能详细，包含省市区街道";

        String userPrompt = String.format(
                "请从以下房源描述中提取信息，返回JSON格式：\n\n" +
                "描述：%s\n\n" +
                "返回格式（严格按此格式）：\n" +
                "{\n" +
                "  \"title\": \"房源标题\",\n" +
                "  \"houseType\": \"户型（如：两室一厅）\",\n" +
                "  \"area\": 面积数字,\n" +
                "  \"floor\": 楼层数字,\n" +
                "  \"totalFloor\": 总楼层数字,\n" +
                "  \"orientation\": \"朝向（如：南、南北通透）\",\n" +
                "  \"rentPrice\": 租金数字,\n" +
                "  \"depositType\": \"押付方式（如：押一付一）\",\n" +
                "  \"province\": \"省份\",\n" +
                "  \"city\": \"城市\",\n" +
                "  \"district\": \"区县\",\n" +
                "  \"detailAddress\": \"详细地址\",\n" +
                "  \"description\": \"详细描述\",\n" +
                "  \"facilities\": [\"设施1\", \"设施2\"]\n" +
                "}\n\n" +
                "直接返回JSON，不要任何其他文字：",
                description
        );

        return generateText(systemPrompt, userPrompt);
    }

    /**
     * AI智能合同核心：先生成合同文本，再转换为可下载的 PDF 文件。
     */
    public byte[] generateRentalContractPDF(Map<String, Object> contractInfo) {
        try {
            // 先生成合同文本
            String contractText = generateRentalContract(contractInfo);
            
            // 将文本转换为PDF
            return convertTextToPDF(contractText, contractInfo);
        } catch (Exception e) {
            throw new RuntimeException("AI生成合同PDF失败：" + e.getMessage());
        }
    }

    /**
     * 将文本转换为PDF
     */
    private byte[] convertTextToPDF(String text, Map<String, Object> contractInfo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // 添加中文字体支持
            String fontPath = "C:/Windows/Fonts/simhei.ttf"; // 黑体
            PdfFont font;
            try {
                font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                // 如果找不到字体文件，使用默认字体
                font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            }
            
            // 设置页边距
            document.setMargins(50, 50, 50, 50);
            
            // 添加标题
            Paragraph title = new Paragraph("房屋租赁合同")
                    .setFont(font)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);
            
            // 添加合同编号和日期
            String contractNo = "HT" + System.currentTimeMillis();
            Paragraph info = new Paragraph(
                    "合同编号：" + contractNo + "\n生成日期：" + 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(20);
            document.add(info);
            
            // 添加合同内容（按段落分割）
            String[] paragraphs = text.split("\n\n");
            for (String para : paragraphs) {
                if (StrUtil.isEmpty(para)) {
                    continue;
                }
                
                Paragraph p = new Paragraph(para.trim())
                        .setFont(font)
                        .setFontSize(11)
                        .setMarginBottom(10)
                        .setFirstLineIndent(20);
                
                // 如果是标题行（包含"一、"、"二、"等），加粗
                if (para.matches("^[一二三四五六七八九十]+、.*")) {
                    p.setBold().setFontSize(12).setFirstLineIndent(0);
                }
                
                document.add(p);
            }
            
            // 添加签名区域
            document.add(new Paragraph("\n\n").setMarginTop(30));
            
            Table signTable = new Table(2);
            signTable.setWidth(UnitValue.createPercentValue(100));
            
            signTable.addCell(new Cell()
                    .add(new Paragraph("甲方（出租方）签字：\n\n\n日期：")
                            .setFont(font).setFontSize(11))
                    .setBorder(Border.NO_BORDER));
            
            signTable.addCell(new Cell()
                    .add(new Paragraph("乙方（承租方）签字：\n\n\n日期：")
                            .setFont(font).setFontSize(11))
                    .setBorder(Border.NO_BORDER));
            
            document.add(signTable);
            
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("转换PDF失败：" + e.getMessage());
        }
    }

    /**
     * AI合同文本生成核心：根据房东、租客和房源信息自动起草租赁合同。
     */
    public String generateRentalContract(Map<String, Object> contractInfo) {
        String systemPrompt = "你是一个专业的法律文书专家，擅长起草房屋租赁合同。" +
                "要求：" +
                "1. 生成完整、正式、专业的房屋租赁合同" +
                "2. 包含所有必要条款，符合法律规范" +
                "3. 语言正式、严谨、清晰" +
                "4. 直接输出合同内容，不要任何说明性文字" +
                "5. 合同格式清晰，条款分明";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请根据以下信息生成一份正式的房屋租赁合同：\n\n");
        
        userPrompt.append("【甲方（出租方）信息】\n");
        userPrompt.append("姓名：").append(contractInfo.get("landlordName")).append("\n");
        userPrompt.append("身份证号：").append(contractInfo.get("landlordIdCard")).append("\n");
        userPrompt.append("联系电话：").append(contractInfo.get("landlordPhone")).append("\n\n");
        
        userPrompt.append("【乙方（承租方）信息】\n");
        userPrompt.append("姓名：").append(contractInfo.get("tenantName")).append("\n");
        userPrompt.append("身份证号：").append(contractInfo.get("tenantIdCard")).append("\n");
        userPrompt.append("联系电话：").append(contractInfo.get("tenantPhone")).append("\n\n");
        
        userPrompt.append("【房屋信息】\n");
        userPrompt.append("地址：").append(contractInfo.get("houseAddress")).append("\n");
        userPrompt.append("户型：").append(contractInfo.get("houseType")).append("\n");
        userPrompt.append("面积：").append(contractInfo.get("area")).append("平方米\n");
        userPrompt.append("楼层：").append(contractInfo.get("floor")).append("\n");
        userPrompt.append("朝向：").append(contractInfo.get("orientation")).append("\n");
        
        Object facilitiesObj = contractInfo.get("facilities");
        if (contractInfo.containsKey("facilities") && facilitiesObj != null 
            && StrUtil.isNotEmpty(facilitiesObj.toString())) {
            userPrompt.append("房屋设施：").append(facilitiesObj).append("\n");
        }
        userPrompt.append("\n");
        
        userPrompt.append("【租赁条款】\n");
        userPrompt.append("租期：").append(contractInfo.get("rentMonths")).append("个月\n");
        userPrompt.append("起租日期：").append(contractInfo.get("startDate")).append("\n");
        userPrompt.append("到期日期：").append(contractInfo.get("endDate")).append("\n");
        userPrompt.append("月租金：").append(contractInfo.get("monthlyRent")).append("元\n");
        userPrompt.append("押金：").append(contractInfo.get("depositAmount")).append("元\n");
        userPrompt.append("押付方式：").append(contractInfo.get("depositType")).append("\n");
        userPrompt.append("付款日：每月").append(contractInfo.get("paymentDay")).append("日\n\n");
        
        userPrompt.append("要求生成包含以下完整条款的合同：\n");
        userPrompt.append("1. 合同标题和编号\n");
        userPrompt.append("2. 甲乙双方基本信息\n");
        userPrompt.append("3. 房屋基本情况\n");
        userPrompt.append("4. 租赁期限\n");
        userPrompt.append("5. 租金及押金\n");
        userPrompt.append("6. 付款方式\n");
        userPrompt.append("7. 甲方权利义务\n");
        userPrompt.append("8. 乙方权利义务\n");
        userPrompt.append("9. 房屋维修与维护\n");
        userPrompt.append("10. 违约责任\n");
        userPrompt.append("11. 合同变更与解除\n");
        userPrompt.append("12. 争议解决\n");
        userPrompt.append("13. 其他约定\n");
        userPrompt.append("14. 合同生效\n\n");
        userPrompt.append("直接输出完整合同内容，不要任何说明：");

        String result = generateText(systemPrompt, userPrompt.toString());
        return cleanAIResponse(result);
    }

    /**
     * AI分析用户偏好并推荐房源
     */
    public String analyzePreferenceAndRecommend(String behaviorDataJson, String availableHousesJson) {
        String systemPrompt = "你是一个专业的房源推荐专家，擅长根据用户行为数据分析其租房偏好并推荐合适的房源。" +
                "要求：" +
                "1. 直接返回JSON格式的数据，不要任何额外说明" +
                "2. 严格按照指定的JSON格式返回" +
                "3. 推荐理由要简洁明了，突出匹配点";

        String userPrompt = String.format(
                "请根据用户的行为数据分析其租房偏好，并推荐最合适的房源。\n\n" +
                "【用户行为数据】\n%s\n\n" +
                "【待推荐房源池】\n%s\n\n" +
                "请分析用户偏好，从房源池中选出6-10个最匹配的房源，按匹配度排序。\n\n" +
                "返回格式（严格按此格式）：\n" +
                "{\n" +
                "  \"userPreference\": {\n" +
                "    \"priceRange\": \"价格区间\",\n" +
                "    \"preferredAreas\": [\"偏好区域\"],\n" +
                "    \"preferredTypes\": [\"偏好户型\"],\n" +
                "    \"preferredFacilities\": [\"偏好设施\"]\n" +
                "  },\n" +
                "  \"recommendations\": [\n" +
                "    {\n" +
                "      \"houseId\": 房源ID,\n" +
                "      \"matchScore\": 匹配分数(0-100),\n" +
                "      \"reason\": \"推荐理由（简短）\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "直接返回JSON，不要任何其他文字：",
                behaviorDataJson, availableHousesJson
        );

        return generateText(systemPrompt, userPrompt);
    }

    /**
     * 聊天话术优化（生成多个版本）
     */
    public List<String> optimizeChatMessageMultiple(String originalMessage, String scene) {
        List<String> results = new ArrayList<>();
        
        // 生成3个不同风格的版本
        String[] styles = {
            "专业正式风格",
            "友好亲切风格", 
            "简洁明了风格"
        };
        
        for (String style : styles) {
            String systemPrompt = "你是一个沟通专家，擅长优化聊天话术。" +
                    "要求：" +
                    "1. 直接输出优化后的消息内容，不要加任何说明、前缀或后缀" +
                    "2. 不要说'以下是优化版本'、'当然可以'等开场白" +
                    "3. 保持原意，语气更友好，表达更专业" +
                    "4. 采用" + style;

            String userPrompt = String.format(
                    "场景：%s\n" +
                    "原始消息：%s\n\n" +
                    "直接输出优化后的消息，不要任何额外说明：",
                    scene, originalMessage
            );
            
            String result = generateText(systemPrompt, userPrompt);
            if (StrUtil.isNotEmpty(result)) {
                // 清理可能的说明性文字
                result = cleanAIResponse(result);
                results.add(result);
            }
        }
        
        // 如果生成失败，返回原消息
        if (results.isEmpty()) {
            results.add(originalMessage);
        }
        
        return results;
    }

    /**
     * 清理AI响应中的说明性文字
     */
    private String cleanAIResponse(String response) {
        if (response == null) {
            return "";
        }
        
        // 移除常见的说明性前缀
        String[] prefixes = {
            "当然可以，以下是优化后的消息：",
            "当然可以，以下是优化版本：",
            "以下是优化后的消息：",
            "以下是优化版本：",
            "优化后的消息如下：",
            "优化版本：",
            "当然，",
            "好的，",
            "以下是",
            "优化后：",
            "优化为：",
            "建议修改为：",
            "可以这样说：",
            "您可以这样表达：",
            "更专业的表达是：",
            "更友好的表达是：",
            "更简洁的表达是："
        };
        
        String cleaned = response.trim();
        for (String prefix : prefixes) {
            if (cleaned.startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length()).trim();
            }
        }
        
        // 移除引号
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.startsWith("\u201C") && cleaned.endsWith("\u201D")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        
        // 移除可能的换行和多余空格
        cleaned = cleaned.replaceAll("\\n+", " ").replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }

    /**
     * 聊天话术优化（单个版本，保留兼容性）
     */
    public String optimizeChatMessage(String originalMessage, String scene) {
        String systemPrompt = "你是一个沟通专家，擅长优化聊天话术，让沟通更专业、友好、有效。" +
                "要求：1. 保持原意 2. 语气更友好 3. 表达更专业 4. 避免冲突";

        String userPrompt = String.format(
                "场景：%s\n" +
                "原始消息：%s\n\n" +
                "请优化这条消息，使其更专业、友好。",
                scene, originalMessage
        );

        return generateText(systemPrompt, userPrompt);
    }

    /**
     * AI合同条款解读
     */
    public String explainContractClause(String contractContent, String question) {
        String systemPrompt = "你是一个专业的法律顾问，擅长用通俗易懂的语言解释合同条款。" +
                "要求：\n" +
                "1. 用简单明了的语言解释，避免法律术语\n" +
                "2. 举例说明，让用户更容易理解\n" +
                "3. 指出条款中的关键点和注意事项\n" +
                "4. 如果涉及权利义务，要明确说明\n" +
                "5. 回答要简洁，控制在200字以内";

        String userPrompt = String.format(
                "合同内容摘要：\n%s\n\n" +
                "用户问题：%s\n\n" +
                "请用通俗易懂的语言回答用户的问题。",
                contractContent.length() > 1000 ? contractContent.substring(0, 1000) + "..." : contractContent,
                question
        );

        return generateText(systemPrompt, userPrompt);
    }

    /**
     * AI智能搜索解析
     */
    public Map<String, Object> parseSearchQuery(String query) {
        String systemPrompt = "你是一个智能搜索助手，擅长理解用户的自然语言搜索意图，并提取结构化的搜索条件。";

        String userPrompt = String.format(
                "用户搜索：%s\n\n" +
                "请分析用户的搜索意图，提取以下信息（如果有的话）：\n" +
                "1. 城市（如：无锡、苏州、南京等）\n" +
                "2. 区域（如：滨湖区、梁溪区等）\n" +
                "3. 户型（如：一室、两室一厅、三室两厅等）\n" +
                "4. 价格范围（最低价和最高价，单位：元/月）\n" +
                "5. 面积范围（最小面积和最大面积，单位：平米）\n" +
                "6. 设施要求（如：空调、冰箱、洗衣机、地铁等）\n" +
                "7. 朝向（如：南、南北通透等）\n" +
                "8. 关键词（其他特征，如：精装、地铁口、学区等）\n\n" +
                "请以JSON格式返回，格式如下：\n" +
                "{\n" +
                "  \"city\": \"城市名\",\n" +
                "  \"district\": \"区域名\",\n" +
                "  \"houseType\": \"户型\",\n" +
                "  \"minPrice\": 最低价格,\n" +
                "  \"maxPrice\": 最高价格,\n" +
                "  \"minArea\": 最小面积,\n" +
                "  \"maxArea\": 最大面积,\n" +
                "  \"facilities\": [\"设施1\", \"设施2\"],\n" +
                "  \"orientation\": \"朝向\",\n" +
                "  \"keywords\": [\"关键词1\", \"关键词2\"]\n" +
                "}\n\n" +
                "注意：\n" +
                "1. 只返回JSON，不要其他文字\n" +
                "2. 如果某个字段无法提取，设置为null\n" +
                "3. 价格和面积要合理推断（如\"3000左右\"可以是2700-3300）\n" +
                "4. 户型要标准化（如\"2室1厅\"转为\"两室一厅\"）",
                query
        );

        String aiResponse = generateText(systemPrompt, userPrompt);
        
        if (StrUtil.isEmpty(aiResponse)) {
            return new JSONObject();
        }

        try {
            // 提取JSON部分
            String jsonStr = aiResponse.trim();
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            }
            jsonStr = jsonStr.trim();
            
            JSONObject result = JSON.parseObject(jsonStr);
            return result;
        } catch (Exception e) {
            log.error("解析AI搜索结果失败", e);
            return new JSONObject();
        }
    }

    /**
     * AI智能定价建议
     */
    public Map<String, Object> getPriceSuggestion(String city, String district, String houseType, 
                                                   Integer area, String facilities, String orientation, String floor) {
        try {
            log.info("=== AI定价建议请求 ===");
            log.info("城市: {}", city);
            log.info("区域: {}", district);
            log.info("户型: {}", houseType);
            log.info("面积: {}", area);
            log.info("设施: {}", facilities);
            log.info("朝向: {}", orientation);
            log.info("楼层: {}", floor);
            
            // 1. 查询数据库中相似房源的价格数据
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.demo.repository.house.entity.House> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            
            wrapper.eq("status", 1); // 只查询可出租的房源
            
            if (StrUtil.isNotEmpty(city)) {
                wrapper.eq("city", city);
            }
            
            // 先尝试精确匹配
            if (StrUtil.isNotEmpty(district)) {
                wrapper.eq("district", district);
            }
            if (StrUtil.isNotEmpty(houseType)) {
                wrapper.eq("house_type", houseType);
            }
            if (area != null) {
                // 面积范围：±20%
                int minArea = (int)(area * 0.8);
                int maxArea = (int)(area * 1.2);
                wrapper.between("area", minArea, maxArea);
                log.info("面积范围: {} - {}", minArea, maxArea);
            }
            
            List<com.example.demo.repository.house.entity.House> similarHouses = houseMapper.selectList(wrapper);
            log.info("精确查询到相似房源数量: {}", similarHouses.size());
            
            // 如果精确查询结果少于3个，放宽条件（不限制区域和户型）
            if (similarHouses.size() < 3) {
                log.info("样本不足，放宽查询条件...");
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.demo.repository.house.entity.House> relaxedWrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                relaxedWrapper.eq("status", 1);
                
                if (StrUtil.isNotEmpty(city)) {
                    relaxedWrapper.eq("city", city);
                }
                
                // 只保留面积条件
                if (area != null) {
                    int minArea = (int)(area * 0.7); // 放宽到±30%
                    int maxArea = (int)(area * 1.3);
                    relaxedWrapper.between("area", minArea, maxArea);
                    log.info("放宽后面积范围: {} - {}", minArea, maxArea);
                }
                
                similarHouses = houseMapper.selectList(relaxedWrapper);
                log.info("放宽条件后查询到房源数量: {}", similarHouses.size());
            }
            
            final List<com.example.demo.repository.house.entity.House> finalSimilarHouses = similarHouses;
            
            // 2. 统计价格数据
            if (finalSimilarHouses.isEmpty()) {
                return createDefaultSuggestion();
            }
            
            List<Integer> prices = new ArrayList<>();
            for (com.example.demo.repository.house.entity.House house : finalSimilarHouses) {
                prices.add(house.getRentPrice().intValue());
            }
            prices.sort(Integer::compareTo);
            
            int minPrice = prices.get(0);
            int maxPrice = prices.get(prices.size() - 1);
            int avgPrice = prices.stream().mapToInt(Integer::intValue).sum() / prices.size();
            int medianPrice = prices.get(prices.size() / 2);
            
            final int finalMinPrice = minPrice;
            final int finalMaxPrice = maxPrice;
            final int finalAvgPrice = avgPrice;
            final int finalMedianPrice = medianPrice;
            final int sampleCount = finalSimilarHouses.size();
            
            // 3. 构建AI提示词
            String systemPrompt = "你是一个房地产定价专家，擅长分析市场数据并给出合理的租金建议。";
            
            String userPrompt = String.format(
                "请根据以下市场数据，为房源提供定价建议：\n\n" +
                "房源信息：\n" +
                "- 城市：%s\n" +
                "- 区域：%s\n" +
                "- 户型：%s\n" +
                "- 面积：%d㎡\n" +
                "- 朝向：%s\n" +
                "- 楼层：%s\n" +
                "- 设施：%s\n\n" +
                "市场数据（同区域同户型相似面积的%d套房源）：\n" +
                "- 最低价：%d元/月\n" +
                "- 最高价：%d元/月\n" +
                "- 平均价：%d元/月\n" +
                "- 中位价：%d元/月\n\n" +
                "请以JSON格式返回定价建议，格式如下：\n" +
                "{\n" +
                "  \"suggestedPrice\": 建议价格（整数），\n" +
                "  \"priceRange\": \"价格区间（如：2800-3200）\",\n" +
                "  \"competitiveness\": \"竞争力评估（高/中/低）\",\n" +
                "  \"analysis\": \"定价分析（50字以内）\",\n" +
                "  \"suggestions\": [\"建议1\", \"建议2\", \"建议3\"]\n" +
                "}\n\n" +
                "注意：\n" +
                "1. 建议价格应该在合理范围内，考虑朝向、楼层、设施等因素\n" +
                "2. 只返回JSON，不要其他文字",
                city, district, houseType, area, orientation, floor, facilities,
                sampleCount, finalMinPrice, finalMaxPrice, finalAvgPrice, finalMedianPrice
            );
            
            // 4. 调用AI生成建议
            String aiResponse = generateText(systemPrompt, userPrompt);
            
            if (StrUtil.isEmpty(aiResponse)) {
                return createDefaultSuggestion(avgPrice, minPrice, maxPrice, similarHouses.size());
            }
            
            // 5. 解析AI返回的JSON
            try {
                // 提取JSON部分
                String jsonStr = aiResponse.trim();
                if (jsonStr.contains("```json")) {
                    jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                } else if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
                jsonStr = jsonStr.trim();
                
                JSONObject result = JSON.parseObject(jsonStr);
                
                // 添加市场数据
                result.put("marketData", new JSONObject() {{
                    put("sampleCount", sampleCount);
                    put("minPrice", finalMinPrice);
                    put("maxPrice", finalMaxPrice);
                    put("avgPrice", finalAvgPrice);
                    put("medianPrice", finalMedianPrice);
                }});
                
                return result;
            } catch (Exception e) {
                log.error("解析AI定价建议失败", e);
                return createDefaultSuggestion(finalAvgPrice, finalMinPrice, finalMaxPrice, sampleCount);
            }
            
        } catch (Exception e) {
            log.error("获取定价建议失败", e);
            return createDefaultSuggestion();
        }
    }
    
    /**
     * AI房源对比分析
     */
    public String compareHouses(List<Map<String, Object>> houses) {
        if (houses == null || houses.isEmpty()) {
            return "请提供需要对比的房源信息";
        }
        
        String systemPrompt = "你是一个专业的房产顾问，擅长分析和对比不同房源的优劣。" +
                "要求：\n" +
                "1. 客观公正地分析每个房源的优缺点\n" +
                "2. 从价格、位置、户型、设施、交通等多维度对比\n" +
                "3. 给出明确的推荐建议\n" +
                "4. 语言简洁明了，易于理解\n" +
                "5. 控制在500字以内";
        
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请帮我对比分析以下").append(houses.size()).append("套房源：\n\n");
        
        for (int i = 0; i < houses.size(); i++) {
            Map<String, Object> house = houses.get(i);
            userPrompt.append("【房源").append(i + 1).append("】\n");
            userPrompt.append("标题：").append(house.get("title")).append("\n");
            userPrompt.append("位置：").append(house.get("address")).append("\n");
            userPrompt.append("户型：").append(house.get("houseType")).append("\n");
            userPrompt.append("面积：").append(house.get("area")).append("㎡\n");
            userPrompt.append("租金：").append(house.get("rentPrice")).append("元/月\n");
            userPrompt.append("押付：").append(house.get("depositType")).append("\n");
            userPrompt.append("朝向：").append(house.get("orientation")).append("\n");
            userPrompt.append("楼层：").append(house.get("floor")).append("\n");
            
            if (house.get("facilities") != null) {
                userPrompt.append("设施：").append(house.get("facilities")).append("\n");
            }
            if (house.get("description") != null) {
                String desc = house.get("description").toString();
                if (desc.length() > 100) {
                    desc = desc.substring(0, 100) + "...";
                }
                userPrompt.append("描述：").append(desc).append("\n");
            }
            userPrompt.append("\n");
        }
        
        userPrompt.append("请从以下维度进行对比分析：\n");
        userPrompt.append("1. 性价比分析\n");
        userPrompt.append("2. 地理位置优劣\n");
        userPrompt.append("3. 居住舒适度\n");
        userPrompt.append("4. 配套设施完善度\n");
        userPrompt.append("5. 综合推荐建议\n\n");
        userPrompt.append("请给出详细的对比分析和推荐意见：");
        
        return generateText(systemPrompt, userPrompt.toString());
    }
    
    /**
     * AI租赁申请审核助手
     */
    public Map<String, Object> analyzeRentalApplication(Map<String, Object> applicationInfo) {
        String systemPrompt = "你是一个专业的租赁风险评估专家，擅长分析租客资料并评估租赁风险。" +
                "要求：\n" +
                "1. 客观分析租客的信用状况和租赁能力\n" +
                "2. 识别潜在风险点\n" +
                "3. 给出明确的审核建议（通过/拒绝/需要补充材料）\n" +
                "4. 提供具体的理由和建议\n" +
                "5. 返回JSON格式的结构化数据\n" +
                "6. 信用评分说明：900+极好，800+优秀，700+良好，600+一般，500+较差，<500差";
        
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请分析以下租赁申请：\n\n");
        
        userPrompt.append("【租客信息】\n");
        userPrompt.append("姓名：").append(applicationInfo.get("tenantName")).append("\n");
        userPrompt.append("实名认证：").append(applicationInfo.get("realnameStatus")).append("\n");
        
        // 信用评分（重要参考）
        Object creditScore = applicationInfo.get("creditScore");
        if (creditScore != null) {
            userPrompt.append("平台信用评分：").append(creditScore).append("分");
            int score = Integer.parseInt(creditScore.toString());
            if (score >= 900) {
                userPrompt.append("（极好）");
            } else if (score >= 800) {
                userPrompt.append("（优秀）");
            } else if (score >= 700) {
                userPrompt.append("（良好）");
            } else if (score >= 600) {
                userPrompt.append("（一般）");
            } else if (score >= 500) {
                userPrompt.append("（较差）");
            } else {
                userPrompt.append("（差）");
            }
            userPrompt.append("\n");
        } else {
            userPrompt.append("平台信用评分：未提供\n");
        }
        
        userPrompt.append("\n");
        
        userPrompt.append("【申请房源】\n");
        userPrompt.append("月租金：").append(applicationInfo.get("rentPrice")).append("元\n");
        userPrompt.append("押金：").append(applicationInfo.get("depositAmount")).append("元\n");
        userPrompt.append("租期：").append(applicationInfo.get("rentMonths")).append("个月\n\n");
        
        Object remarks = applicationInfo.get("remarks");
        if (remarks != null && StrUtil.isNotEmpty(remarks.toString())) {
            userPrompt.append("【申请理由】\n");
            userPrompt.append(remarks).append("\n\n");
        }
        
        userPrompt.append("请以JSON格式返回分析结果，格式如下：\n");
        userPrompt.append("{\n");
        userPrompt.append("  \"recommendation\": \"通过/拒绝/需要补充材料\",\n");
        userPrompt.append("  \"riskLevel\": \"低/中/高\",\n");
        userPrompt.append("  \"riskScore\": 风险评分(0-100),\n");
        userPrompt.append("  \"analysis\": \"综合分析（100字以内）\",\n");
        userPrompt.append("  \"strengths\": [\"优势1\", \"优势2\"],\n");
        userPrompt.append("  \"concerns\": [\"风险点1\", \"风险点2\"],\n");
        userPrompt.append("  \"suggestions\": [\"建议1\", \"建议2\"]\n");
        userPrompt.append("}\n\n");
        userPrompt.append("只返回JSON，不要其他文字：");
        
        String aiResponse = generateText(systemPrompt, userPrompt.toString());
        
        if (StrUtil.isEmpty(aiResponse)) {
            return createDefaultAuditResult();
        }
        
        try {
            // 提取JSON部分
            String jsonStr = aiResponse.trim();
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            }
            jsonStr = jsonStr.trim();
            
            JSONObject result = JSON.parseObject(jsonStr);
            return result;
        } catch (Exception e) {
            log.error("解析AI审核结果失败", e);
            return createDefaultAuditResult();
        }
    }
    
    /**
     * 创建默认审核结果
     */
    private Map<String, Object> createDefaultAuditResult() {
        JSONObject result = new JSONObject();
        result.put("recommendation", "需要补充材料");
        result.put("riskLevel", "中");
        result.put("riskScore", 50);
        result.put("analysis", "信息不足，无法进行准确评估，建议补充更多资料");
        result.put("strengths", new String[]{"已完成实名认证"});
        result.put("concerns", new String[]{"缺少详细的收入证明", "租赁历史信息不完整"});
        result.put("suggestions", new String[]{
            "建议租客提供收入证明或工作证明",
            "可以要求提供前房东的推荐信",
            "建议适当提高押金金额以降低风险"
        });
        return result;
    }
    
    /**
     * 创建默认定价建议（无市场数据）
     */
    private Map<String, Object> createDefaultSuggestion() {
        JSONObject result = new JSONObject();
        result.put("suggestedPrice", 0);
        result.put("priceRange", "暂无数据");
        result.put("competitiveness", "未知");
        result.put("analysis", "暂无足够的市场数据进行分析，建议参考周边同类房源价格");
        result.put("suggestions", new String[]{
            "建议调研周边同类房源的租金水平",
            "可以先设置一个试探性价格，根据市场反馈调整",
            "考虑房源的独特优势，适当调整定价"
        });
        result.put("marketData", new JSONObject() {{
            put("sampleCount", 0);
            put("minPrice", 0);
            put("maxPrice", 0);
            put("avgPrice", 0);
            put("medianPrice", 0);
        }});
        return result;
    }
    
    /**
     * 创建默认定价建议（有市场数据）
     */
    private Map<String, Object> createDefaultSuggestion(int avgPrice, int minPrice, int maxPrice, int sampleCount) {
        JSONObject result = new JSONObject();
        result.put("suggestedPrice", avgPrice);
        result.put("priceRange", minPrice + "-" + maxPrice);
        result.put("competitiveness", "中");
        result.put("analysis", String.format("根据%d套相似房源数据，建议定价在平均水平附近", sampleCount));
        result.put("suggestions", new String[]{
            "建议价格在" + minPrice + "-" + maxPrice + "元之间",
            "可以根据房源具体情况微调价格",
            "关注市场反馈，适时调整定价策略"
        });
        result.put("marketData", new JSONObject() {{
            put("sampleCount", sampleCount);
            put("minPrice", minPrice);
            put("maxPrice", maxPrice);
            put("avgPrice", avgPrice);
            put("medianPrice", avgPrice);
        }});
        return result;
    }
}
