package com.fulinlin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.io.resource.ResourceUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.xbill.DNS.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @program: github-dns
 * @author: fulin
 * @create: 2021-03-03 17:14
 **/
@Slf4j
public class GetNewestGithubDns {

    private static final String DNS_SERVER = "8.8.8.8";

    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        List<GithubDns> result = new ArrayList<>();
        List<GithubDns> domainList = initGithubDnsList();
        log.info("=================================================================");
        for (GithubDns githubDns : domainList) {
            Resolver resolver = new SimpleResolver(DNS_SERVER);
            Lookup lookup = new Lookup(githubDns.getHostname(), Type.A);
            lookup.setResolver(resolver);
            Cache cache = new Cache();
            lookup.setCache(cache);
            lookup.run();
            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                Record[] records = lookup.getAnswers();
                for (Record record : records) {
                    result.add(new GithubDns(githubDns.getHostname(), record.rdataToString()));
                }
                String address = Stream.of(records).map(Record::rdataToString).collect(Collectors.joining(","));
                log.info("hostname: {} , address：{} ", githubDns.getHostname(), address);
            }
        }
        log.info("=================================================================");
        generateGithubDnsHosts(result);
    }

    private static void generateGithubDnsHosts(List<GithubDns> list) {
        String projectPath = System.getProperty("user.dir");
        String readmePath = projectPath + File.separator + "README.md";
        File file = new File(readmePath);
        FileReader fileReader = new FileReader(file);
        String reader = fileReader.readString();
        int i = StringUtils.indexOf(reader, "# update");
        String substring = StringUtils.substring(reader, 0, i);
        FileWriter writer = new FileWriter(file);
        writer.write(substring);
        FileUtil.appendUtf8String("# update " + formatter.format(LocalDateTime.now()) + "\n", file);
        FileUtil.appendUtf8String("```" + "\n", file);
        OptionalInt max = list.stream().map(GithubDns::getIpaddress).mapToInt(String::length).max();
        list.forEach(val -> {
            FileUtil.appendUtf8String(val.getIpaddress() + completionFormatter(val, max.orElse(0)) + val.getHostname() + "\n", file);
        });
        FileUtil.appendUtf8String("```", file);
    }

    private static String completionFormatter(GithubDns githubDns, int maxIpLength) {
        int length = githubDns.getIpaddress().length();
        int completion = maxIpLength - length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15 + completion; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private static List<GithubDns> initGithubDnsList() throws IOException {
        BufferedReader githubDomains = ResourceUtil.getUtf8Reader("GithubDomains");
        List<GithubDns> list = new ArrayList<>();
        String line;
        while ((line = githubDomains.readLine()) != null) {
            list.add(new GithubDns(line));
        }
        githubDomains.close();
        return list;
    }


}

@Data
class GithubDns {

    private String hostname;

    private String ipaddress;


    public GithubDns(String hostname) {
        this.hostname = hostname;
    }

    public GithubDns(String hostname, String ipaddress) {
        this.hostname = hostname;
        this.ipaddress = ipaddress;
    }
}
