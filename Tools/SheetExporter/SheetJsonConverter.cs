using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace COU.SheetExporter
{
    public sealed class ConversionResult
    {
        public string Json { get; internal set; }
        public int RowCount { get; internal set; }
        public List<string> Errors { get; } = new List<string>();
        public bool Success => Errors.Count == 0 && Json != null;
    }

    public static class SheetJsonConverter
    {
        public static ConversionResult Convert(string tsv)
        {
            var result = new ConversionResult();
            List<List<string>> rows;
            try { rows = ReadTsv(tsv ?? string.Empty); }
            catch (FormatException e) { result.Errors.Add(e.Message); return result; }
            if (rows.Count < 3)
            {
                result.Errors.Add("설명, 필드명, 자료형의 3개 헤더 행이 필요합니다.");
                return result;
            }
            if (rows[0][0].TrimStart().StartsWith("<", StringComparison.Ordinal))
            {
                result.Errors.Add("TSV 대신 HTML을 받았습니다. 시트 공유 권한과 주소를 확인하세요.");
                return result;
            }
            int width = rows.Take(3).Max(r => r.Count);
            var columns = new List<int>();
            var names = new HashSet<string>(StringComparer.Ordinal);
            for (int c = 0; c < width; c++)
            {
                string name = Cell(rows[1], c).Trim();
                string type = Cell(rows[2], c).Trim();
                if (Ignore(Cell(rows[0], c))) continue;
                if (name.Length == 0 && type.Length == 0 && Cell(rows[0], c).Length == 0) continue;
                if (name.Length == 0) result.Errors.Add($"2행 {c + 1}열: 필드명이 비어 있습니다.");
                else if (!names.Add(name)) result.Errors.Add($"2행 {c + 1}열: 중복 필드명 '{name}'.");
                if (!Supported(type)) result.Errors.Add($"3행 {c + 1}열: 지원하지 않는 자료형 '{type}'.");
                columns.Add(c);
            }
            if (columns.Count == 0) result.Errors.Add("변환할 데이터 열이 없습니다.");
            if (result.Errors.Count > 0) return result;
            var data = new JArray();
            for (int r = 3; r < rows.Count; r++)
            {
                var row = rows[r];
                if (row.All(string.IsNullOrWhiteSpace) || Ignore(Cell(row, 0))) continue;
                if (row.Skip(width).Any(v => !string.IsNullOrWhiteSpace(v)))
                    result.Errors.Add($"{r + 1}행: 헤더 범위를 벗어난 데이터가 있습니다.");
                var item = new JObject();
                foreach (int c in columns)
                {
                    string name = Cell(rows[1], c).Trim();
                    string type = Cell(rows[2], c).Trim();
                    try { item[name] = Value(Cell(row, c), type); }
                    catch (FormatException e)
                    { result.Errors.Add($"{r + 1}행 {c + 1}열 ({name}): {e.Message}"); }
                }
                data.Add(item);
            }
            result.RowCount = data.Count;
            if (result.Errors.Count == 0)
                result.Json = new JObject { ["datas"] = data }.ToString(Formatting.Indented);
            return result;
        }

        private static string Cell(List<string> row, int index) => index < row.Count ? row[index] : "";
        private static bool Ignore(string value) => value.Trim().Equals("DB_IGNORE", StringComparison.OrdinalIgnoreCase);
        private static bool Supported(string type) => type == "string" || type == "int" || type == "long"
            || type == "float" || type == "double" || type == "bool"
            || type == "string[]" || type == "int[]" || type == "float[]";

        private static JToken Value(string raw, string type)
        {
            if (type == "string") return new JValue(raw);
            string value = raw.Trim();
            if (type.EndsWith("[]", StringComparison.Ordinal))
            {
                var array = new JArray();
                if (value.Length == 0) return array;
                string elementType = type.Substring(0, type.Length - 2);
                foreach (string part in value.Split(',')) array.Add(Value(part.Trim(), elementType));
                return array;
            }
            var culture = CultureInfo.InvariantCulture;
            if (type == "int" && int.TryParse(value, NumberStyles.Integer, culture, out int i)) return new JValue(i);
            if (type == "long" && long.TryParse(value, NumberStyles.Integer, culture, out long l)) return new JValue(l);
            if (type == "float" && float.TryParse(value, NumberStyles.Float, culture, out float f)
                && !float.IsNaN(f) && !float.IsInfinity(f)) return new JValue(f);
            if (type == "double" && double.TryParse(value, NumberStyles.Float, culture, out double d)
                && !double.IsNaN(d) && !double.IsInfinity(d)) return new JValue(d);
            if (type == "bool" && bool.TryParse(value, out bool b)) return new JValue(b);
            throw new FormatException($"'{value}' 값을 {type} 형식으로 변환할 수 없습니다. 빈 숫자/bool 셀도 허용하지 않습니다.");
        }

        // Supports quoted tabs, quoted line breaks, escaped quotes and CRLF.
        private static List<List<string>> ReadTsv(string text)
        {
            var rows = new List<List<string>>();
            var row = new List<string>();
            var field = new StringBuilder();
            bool quoted = false, closed = false;
            text = text.TrimStart('﻿');
            for (int i = 0; i < text.Length; i++)
            {
                char ch = text[i];
                if (quoted)
                {
                    if (ch == '"')
                    {
                        if (i + 1 < text.Length && text[i + 1] == '"') { field.Append('"'); i++; }
                        else { quoted = false; closed = true; }
                    }
                    else field.Append(ch);
                    continue;
                }
                if (ch == '\t' || ch == '\r' || ch == '\n')
                {
                    row.Add(field.ToString()); field.Clear(); closed = false;
                    if (ch != '\t')
                    {
                        rows.Add(row); row = new List<string>();
                        if (ch == '\r' && i + 1 < text.Length && text[i + 1] == '\n') i++;
                    }
                }
                else if (closed) throw new FormatException($"{rows.Count + 1}행: 닫는 따옴표 뒤에 구분자가 필요합니다.");
                else if (ch == '"' && field.Length == 0) quoted = true;
                else field.Append(ch);
            }
            if (quoted) throw new FormatException($"{rows.Count + 1}행: 셀의 따옴표가 닫히지 않았습니다.");
            row.Add(field.ToString()); rows.Add(row);
            return rows;
        }
    }
}
