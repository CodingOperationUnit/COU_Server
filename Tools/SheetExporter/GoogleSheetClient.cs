using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;

namespace COU.SheetExporter
{
    public sealed class SheetInfo
    {
        public string Name;
        public int Id;
    }

    public static class GoogleSheetClient
    {
        private static readonly HttpClient Http = new HttpClient { Timeout = TimeSpan.FromSeconds(30) };

        public static bool TryGetSpreadsheetId(string url, out string id)
        {
            id = null;
            if (!Uri.TryCreate(url.Trim(), UriKind.Absolute, out var uri)
                || uri.Scheme != "https" || uri.Host != "docs.google.com") return false;
            var match = Regex.Match(uri.AbsolutePath, @"^/spreadsheets/d/([a-zA-Z0-9_-]+)(?:/|$)");
            if (!match.Success) return false;
            id = match.Groups[1].Value;
            return true;
        }

        public static async Task<List<SheetInfo>> FetchSheetList(string apiUrl)
        {
            if (!Uri.TryCreate(apiUrl.Trim(), UriKind.Absolute, out var uri)
                || uri.Scheme != "https" || uri.Host != "script.google.com"
                || !uri.AbsolutePath.EndsWith("/exec", StringComparison.Ordinal))
                throw new FormatException("Apps Script의 https://script.google.com/.../exec 배포 주소가 필요합니다.");
            string text = await Download(uri.AbsoluteUri);
            try
            {
                var array = JObject.Parse(text)["sheetData"] as JArray;
                if (array == null) throw new FormatException("sheetData 배열이 없습니다.");
                var sheets = new List<SheetInfo>();
                foreach (var entry in array)
                {
                    string name = entry.Value<string>("sheetName");
                    var idToken = entry["sheetId"];
                    if (string.IsNullOrWhiteSpace(name) || idToken == null || idToken.Type != JTokenType.Integer)
                        throw new FormatException("sheetName 또는 sheetId가 올바르지 않습니다.");
                    sheets.Add(new SheetInfo { Name = name, Id = idToken.Value<int>() });
                }
                if (sheets.Count == 0) throw new FormatException("시트 목록이 비어 있습니다.");
                return sheets;
            }
            catch (Exception e) { throw new FormatException("시트 목록 해석 실패: " + e.Message); }
        }

        public static Task<string> DownloadSheetTsv(string spreadsheetUrl, int gid)
        {
            if (!TryGetSpreadsheetId(spreadsheetUrl, out string id))
                throw new FormatException("올바른 Google 스프레드시트 주소가 필요합니다.");
            return Download($"https://docs.google.com/spreadsheets/d/{id}/export?format=tsv&gid={gid}");
        }

        private static async Task<string> Download(string url)
        {
            using (var response = await Http.GetAsync(url))
            {
                if (!response.IsSuccessStatusCode)
                    throw new HttpRequestException($"다운로드 실패 (HTTP {(int)response.StatusCode}): {url}");
                return await response.Content.ReadAsStringAsync();
            }
        }
    }
}
