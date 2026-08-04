// Generate 17 business modules skeleton with entity/mapper for dayan-server
// Usage: node gen-modules.js
const fs = require('fs');
const path = require('path');

const MODULES_ROOT = path.resolve(__dirname, '../dayan-server/dayan-modules');

// Module registry: dirName -> { pkg, cnName }
const MODULES = [
  { dir: 'dayan-module-system', pkg: 'system', cn: '系统' },
  { dir: 'dayan-module-organ', pkg: 'organ', cn: '核心' },
  { dir: 'dayan-module-butler', pkg: 'butler', cn: '养老管家' },
  { dir: 'dayan-module-supplier', pkg: 'supplier', cn: '供应商' },
  { dir: 'dayan-module-park', pkg: 'park', cn: '养老机构' },
  { dir: 'dayan-module-scene', pkg: 'scene', cn: '场景' },
  { dir: 'dayan-module-channel', pkg: 'channel', cn: '渠道' },
  { dir: 'dayan-module-agent', pkg: 'agent', cn: '代理人' },
  { dir: 'dayan-module-client', pkg: 'client', cn: '客户' },
  { dir: 'dayan-module-equity', pkg: 'equity', cn: '权益' },
  { dir: 'dayan-module-service', pkg: 'service', cn: '服务' },
  { dir: 'dayan-module-goods', pkg: 'goods', cn: '商品' },
  { dir: 'dayan-module-content', pkg: 'content', cn: '内容' },
  { dir: 'dayan-module-course', pkg: 'course', cn: '课程' },
  { dir: 'dayan-module-order', pkg: 'order', cn: '订单' },
  { dir: 'dayan-module-finance', pkg: 'finance', cn: '结算' },
  { dir: 'dayan-module-distributor', pkg: 'distributor', cn: '分销商' },
];

function writePom(mod) {
  const content = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.dayan</groupId>
        <artifactId>dayan-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>${mod.dir}</artifactId>
    <name>${mod.dir}</name>
    <description>${mod.cn}域</description>
    <dependencies>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-core</artifactId></dependency>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-mybatis</artifactId></dependency>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-security</artifactId></dependency>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-log</artifactId></dependency>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-swagger</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
        <dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId></dependency>
    </dependencies>
</project>
`;
  fs.writeFileSync(path.join(MODULES_ROOT, mod.dir, 'pom.xml'), content);
}

// Generate empty package-info for placeholder packages
const PLACEHOLDER_PKGS = [
  'controller/admin',
  'controller/channel',
  'controller/agent',
  'controller/client',
  'controller/supplier',
  'controller/distributor',
  'controller/open',
  'service/impl',
  'dto',
  'vo',
  'converter',
  'enums',
  'statemachine',
];

function writePackageInfo(mod, relPkgPath, leafName) {
  const fullPkg = `com.dayan.${mod.pkg}.${relPkgPath.replace(/\//g, '.')}`;
  const filePath = path.join(
    MODULES_ROOT, mod.dir,
    'src/main/java',
    ...fullPkg.split('.'),
    'package-info.java'
  );
  const content = `/**
 * ${mod.cn}域 - ${leafName} 包（占位，待业务代码填充）。
 */
package ${fullPkg};
`;
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content);
}

// Mapping rules: BIGINT->Long, INT/TINYINT->Integer, VARCHAR/TEXT/LONGTEXT/CHAR->String,
//   DATETIME/TIMESTAMP->LocalDateTime, DATE->LocalDate, TIME->LocalTime,
//   DECIMAL->BigDecimal, JSON->String
function mapType(rawType) {
  const t = rawType.toLowerCase();
  if (t.startsWith('bigint')) return 'Long';
  if (t.startsWith('int') || t.startsWith('tinyint') || t.startsWith('smallint') || t.startsWith('mediumint')) return 'Integer';
  if (t.startsWith('decimal') || t.startsWith('numeric') || t.startsWith('float') || t.startsWith('double')) return 'BigDecimal';
  if (t.startsWith('datetime') || t.startsWith('timestamp')) return 'LocalDateTime';
  if (t.startsWith('date')) return 'LocalDate';
  if (t.startsWith('time')) return 'LocalTime';
  if (t.startsWith('varchar') || t.startsWith('char') || t.startsWith('text') || t.startsWith('longtext') || t.startsWith('mediumtext') || t.startsWith('tinytext') || t.startsWith('json') || t.startsWith('enum')) return 'String';
  if (t.startsWith('blob') || t.startsWith('binary') || t.startsWith('varbinary')) return 'byte[]';
  return 'String';
}

// snake_case -> camelCase
function toCamel(s) {
  return s.replace(/_([a-z0-9])/g, (_, c) => c.toUpperCase());
}
// snake_case_table -> PascalCase class
function toPascal(s) {
  const camel = toCamel(s);
  return camel.charAt(0).toUpperCase() + camel.slice(1);
}

// entityClassList: array of { tableName, fields: [{name, type, comment}] }
function writeEntity(mod, tableName, fields) {
  const className = toPascal(tableName);
  const fullPkg = `com.dayan.${mod.pkg}.entity`;
  const filePath = path.join(
    MODULES_ROOT, mod.dir,
    'src/main/java',
    ...fullPkg.split('.'),
    `${className}.java`
  );

  const needBigDecimal = fields.some(f => f.type === 'BigDecimal');
  const needLocalDate = fields.some(f => f.type === 'LocalDate');
  const needLocalTime = fields.some(f => f.type === 'LocalTime');
  const needLocalDateTime = fields.some(f => f.type === 'LocalDateTime');

  let imports = `import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
`;
  if (needBigDecimal) imports += `import java.math.BigDecimal;\n`;
  if (needLocalDate) imports += `import java.time.LocalDate;\n`;
  if (needLocalTime) imports += `import java.time.LocalTime;\n`;
  if (needLocalDateTime) imports += `import java.time.LocalDateTime;\n`;

  const fieldLines = fields.map(f => {
    const comment = (f.comment || '').replace(/\s+/g, ' ').trim();
    const cmt = comment ? `\n    /** ${comment} */` : '';
    return `${cmt}\n    private ${f.type} ${toCamel(f.name)};`;
  }).join('\n');

  const content = `package ${fullPkg};

${imports}/**
 * 表 ${tableName} 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("${tableName}")
public class ${className} extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

${fieldLines}
}
`;
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content);
}

function writeMapper(mod, tableName) {
  const className = toPascal(tableName);
  const entityFqp = `com.dayan.${mod.pkg}.entity.${className}`;
  const fullPkg = `com.dayan.${mod.pkg}.mapper`;
  const filePath = path.join(
    MODULES_ROOT, mod.dir,
    'src/main/java',
    ...fullPkg.split('.'),
    `${className}Mapper.java`
  );
  const content = `package ${fullPkg};

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import ${entityFqp};
import org.apache.ibatis.annotations.Mapper;

/**
 * ${tableName} 数据访问层。
 */
@Mapper
public interface ${className}Mapper extends BaseMapper<${className}> {
}
`;
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content);
}

// Build module: pom, package-info placeholders (entity & mapper dirs created when writing files)
function buildModuleSkeleton(mod) {
  // module dir
  fs.mkdirSync(path.join(MODULES_ROOT, mod.dir), { recursive: true });
  writePom(mod);
  // placeholder packages (NOT entity/mapper - those come with classes)
  for (const rel of PLACEHOLDER_PKGS) {
    const parts = rel.split('/');
    const leaf = parts[parts.length - 1];
    writePackageInfo(mod, rel, leaf);
  }
}

module.exports = {
  MODULES_ROOT, MODULES, PLACEHOLDER_PKGS,
  buildModuleSkeleton, writeEntity, writeMapper, toPascal, toCamel, mapType,
};
