package cn.pridezh.tagexplore.core.controller;

import cn.pridezh.tagexplore.core.domain.common.PageDTO;
import cn.pridezh.tagexplore.core.domain.common.Result;
import cn.pridezh.tagexplore.core.config.properties.AppProperties;
import cn.pridezh.tagexplore.core.domain.dto.ResourceUpdateDTO;
import cn.pridezh.tagexplore.core.domain.po.Resource;
import cn.pridezh.tagexplore.core.domain.vo.ResourceItemVO;
import cn.pridezh.tagexplore.core.domain.vo.ResourceVO;
import cn.pridezh.tagexplore.core.exception.ServiceException;
import cn.pridezh.tagexplore.core.service.ResourceService;
import cn.pridezh.tagexplore.core.util.XORUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author PrideZH
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/resource")
public class ResourceController {

    private final ResourceService resourceService;

    private final AppProperties appProperties;

    @PostMapping("")
    public Result<List<ResourceItemVO>> post(MultipartFile[] files, HttpServletRequest request)
            throws Exception {
        JSONParser jsonParser = new JSONParser(request.getParameter("tags"));

        List<Long> tagIds = jsonParser.parseArray().stream()
                .map(tagId -> Long.parseLong(tagId.toString())).toList();
        return Result.success(resourceService.post(files, tagIds));
    }

    @PostMapping("/cover")
    public Result<Void> uploadCover(MultipartFile file, String id) throws IOException {
        resourceService.uploadCover(file, Long.valueOf(id));
        return Result.success(null);
    }

    @GetMapping("")
    public Result<IPage<ResourceItemVO>> list(@RequestParam(required = false) List<String> tags, PageDTO pageDTO) {
        return Result.success(resourceService.page(Optional.ofNullable(tags)
                .map(data -> data.stream().map(Long::valueOf).toList())
                .orElse(null), pageDTO));
    }

    @GetMapping("/{id:\\d+}")
    public Result<ResourceVO> get(@PathVariable("id") String id) throws Exception {
        return Result.success(resourceService.get(Long.valueOf(id)));
    }

    @GetMapping(value = "/cover/{id:\\d+}",
            produces = { MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE })
    public byte[] getCover(@PathVariable("id") String id) {
        Resource resource = resourceService.getById(Long.valueOf(id));
        if (resource == null || resource.getCover() == null) {
            return new byte[] {};
        }

        String password = appProperties.getAuth().getPassword();

        if (StringUtils.isNotBlank(password)) {
            return XORUtils.execute(resource.getCover(), password, false);
        } else {
            return resource.getCover();
        }
    }

    @GetMapping(value = "/images/{id:\\d+}",
            produces = { MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE })
    public byte[] getImage(@PathVariable("id") String id) throws Exception {
        Resource resource = resourceService.getById(Long.valueOf(id));
        if (resource == null) {
            return new byte[] {};
        }
        String filename = resource.getName();

        String password = appProperties.getAuth().getPassword();

        File file = new File(appProperties.getRepository() + "/" + filename);
        if (!file.exists()) {
            throw new ServiceException(1001, "file:" + filename + " not exists");
        }

        FileInputStream inputStream = new FileInputStream(file);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes, 0, inputStream.available());

        if (StringUtils.isNotBlank(password)) {
            return XORUtils.execute(bytes, password);
        }
        return bytes;
    }

    @GetMapping(value = "/videos/{id:\\d+}")
    public void getVideo(@PathVariable("id") String id,
                         HttpServletRequest request, HttpServletResponse response) throws Exception {
        Resource resource = resourceService.getById(Long.valueOf(id));
        if (resource == null) {
            return;
        }
        String filename = resource.getName();

        String password = appProperties.getAuth().getPassword();

        // ????????????
        response.reset();
        // ????????????????????????
        OutputStream outputStream = response.getOutputStream();

        File file = new File(appProperties.getRepository() + "/" + filename);
        if (file.exists()) {
            // ??????????????????????????????
            RandomAccessFile targetFile = new RandomAccessFile(file, "r");
            long fileLength = targetFile.length();
            // ???????????????????????????????????????
            String rangeString = request.getHeader("Range");
            if (rangeString != null) {
                long range = Long.parseLong(rangeString.substring(rangeString.indexOf("=") + 1, rangeString.indexOf("-")));
                // ??????????????????
                response.setHeader("Content-Type", "video/mp4");
                // ???????????????????????????????????????
                response.setHeader("Content-Length", String.valueOf(fileLength - range));
                // ???????????????????????????????????????
                response.setHeader("Content-Range", "bytes " + range + "-" + (fileLength - 1) + "/" + fileLength);
                // ??????????????????206????????????200
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                // ??????????????????????????????????????????????????????
                targetFile.seek(range);
            } else {
                // ?????????
                response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".mp4" );
                // ????????????
                response.setHeader("Content-Length", String.valueOf(fileLength));
                // ????????????
                response.setHeader("Content-Type","application/octet-stream");
            }
            byte[] cache = new byte[1024 * 300];
            int len;
            while ((len = targetFile.read(cache)) != -1){
                if (StringUtils.isNotBlank(password)) {
                    byte[] res = XORUtils.execute(cache, password);
                    outputStream.write(res, 0, len);
                } else {
                    outputStream.write(cache, 0, len);
                }
            }
        } else {
            String message = "file:" + filename + " not exists";
            response.setHeader("Content-Type", "application/json");
            outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        }
        outputStream.flush();
        outputStream.close();
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> put(
            @PathVariable("id") String id,
            @Validated @RequestBody ResourceUpdateDTO resourceUpdateDTO) throws Exception {
        resourceUpdateDTO.setId(Long.valueOf(id));
        resourceService.put(resourceUpdateDTO);
        return Result.success(null);
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable("id") String id) {
        resourceService.delete(Long.valueOf(id));
        return Result.success(null);
    }

    @DeleteMapping("/{id:\\d+}/cover")
    public Result<Void> delCover(@PathVariable("id") String id) {
        resourceService.delCover(Long.valueOf(id));
        return Result.success(null);
    }

}
