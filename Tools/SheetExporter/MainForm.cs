using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace COU.SheetExporter
{
    // 시트의 모든 탭을 JSON으로 변환해 저장 위치(선택 시 클라이언트 데이터 위치)에 쓴다. 저장 위치에 version.txt가 있어야 한다.
    // 서버가 내려줄 테이블을 알 수 있도록 탭 이름 목록을 저장 위치의 tables.txt에 함께 쓴다.
    internal sealed class MainForm : Form
    {
        private const string VersionFileName = "version.txt";
        private const string TableListFileName = "tables.txt";

        private readonly TextBox saveFolderBox = new TextBox { Anchor = AnchorStyles.Left | AnchorStyles.Right };
        private readonly TextBox spreadsheetUrlBox = new TextBox { Anchor = AnchorStyles.Left | AnchorStyles.Right };
        private readonly TextBox apiUrlBox = new TextBox { Anchor = AnchorStyles.Left | AnchorStyles.Right };
        private readonly TextBox clientFolderBox = new TextBox { Anchor = AnchorStyles.Left | AnchorStyles.Right };
        private readonly CheckBox clientCopyCheck = new CheckBox { Text = "클라이언트 복제", AutoSize = true, Anchor = AnchorStyles.Left };
        private readonly ComboBox versionBox = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, Width = 220, Anchor = AnchorStyles.Left };
        private readonly Button exportButton = new Button { Text = "Export", AutoSize = true, Padding = new Padding(12, 2, 12, 2) };
        private readonly TextBox logBox = new TextBox
        {
            Multiline = true, ReadOnly = true, ScrollBars = ScrollBars.Both, WordWrap = false, Dock = DockStyle.Fill
        };

        public MainForm()
        {
            Text = "COU Sheet Exporter";
            ClientSize = new Size(760, 520);
            MinimumSize = new Size(560, 400);
            StartPosition = FormStartPosition.CenterScreen;

            var layout = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 3, Padding = new Padding(10) };
            layout.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            layout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
            layout.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            AddRow(layout, "저장 위치", saveFolderBox, BrowseButton(saveFolderBox));
            AddRow(layout, "구글 시트 주소", spreadsheetUrlBox, null);
            AddRow(layout, "Apps Script 주소", apiUrlBox, null);
            AddRow(layout, "클라이언트 데이터 위치", clientFolderBox, BrowseButton(clientFolderBox));

            var options = new FlowLayoutPanel { AutoSize = true, WrapContents = false, Margin = new Padding(0) };
            options.Controls.Add(clientCopyCheck);
            options.Controls.Add(new Label { Text = "데이터 버전", AutoSize = true, Anchor = AnchorStyles.Left, Margin = new Padding(20, 3, 3, 3) });
            options.Controls.Add(versionBox);
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            layout.Controls.Add(options, 1, layout.RowStyles.Count - 1);
            layout.Controls.Add(exportButton, 2, layout.RowStyles.Count - 1);

            layout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            layout.Controls.Add(logBox, 0, layout.RowStyles.Count - 1);
            layout.SetColumnSpan(logBox, 3);
            Controls.Add(layout);

            versionBox.Items.AddRange(new object[] { "자동 (변경 시 셋째 자리 +1)", "둘째 자리 +1", "첫째 자리 +1" });
            versionBox.SelectedIndex = 0;

            var settings = ExporterSettings.Load();
            saveFolderBox.Text = settings.SaveFolder;
            spreadsheetUrlBox.Text = settings.SpreadsheetUrl;
            apiUrlBox.Text = settings.ApiUrl;
            clientFolderBox.Text = settings.ClientFolder;
            clientCopyCheck.Checked = settings.ClientCopy;

            exportButton.Click += OnExportClick;
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            SaveSettings();
            base.OnFormClosing(e);
        }

        private static void AddRow(TableLayoutPanel layout, string label, TextBox box, Button button)
        {
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            int row = layout.RowStyles.Count - 1;
            layout.Controls.Add(new Label { Text = label, AutoSize = true, Anchor = AnchorStyles.Left }, 0, row);
            layout.Controls.Add(box, 1, row);
            if (button != null) layout.Controls.Add(button, 2, row);
            else layout.SetColumnSpan(box, 2);
        }

        private Button BrowseButton(TextBox box)
        {
            var button = new Button { Text = "...", AutoSize = true, AutoSizeMode = AutoSizeMode.GrowAndShrink };
            button.Click += (s, e) =>
            {
                using (var dialog = new FolderBrowserDialog { SelectedPath = box.Text.Trim().Trim('"') })
                    if (dialog.ShowDialog(this) == DialogResult.OK) box.Text = dialog.SelectedPath;
            };
            return button;
        }

        private void SaveSettings() => new ExporterSettings
        {
            SaveFolder = saveFolderBox.Text,
            SpreadsheetUrl = spreadsheetUrlBox.Text,
            ApiUrl = apiUrlBox.Text,
            ClientFolder = clientFolderBox.Text,
            ClientCopy = clientCopyCheck.Checked
        }.Save();

        private async void OnExportClick(object sender, EventArgs e)
        {
            SaveSettings();
            exportButton.Enabled = false;
            logBox.Clear();
            try { await Export(); }
            catch (Exception ex) { Log("실패: " + ex.Message); }
            finally { exportButton.Enabled = true; }
        }

        private async Task Export()
        {
            string serverFolder = saveFolderBox.Text.Trim().Trim('"');
            string clientFolder = clientCopyCheck.Checked ? clientFolderBox.Text.Trim().Trim('"') : null;
            string versionFile = Path.Combine(serverFolder, VersionFileName);
            if (!File.Exists(versionFile))
                throw new IOException($"{versionFile} 파일이 없습니다. 저장 위치를 확인하세요.");
            if (clientFolder != null && !Directory.Exists(clientFolder))
                throw new IOException($"{clientFolder} 폴더가 없습니다. 클라이언트 데이터 위치를 확인하세요.");
            string current = File.ReadAllText(versionFile).Trim();
            int[] version = ParseVersion(current);

            var tables = new List<(string Name, string Json)>();
            bool failed = false;
            foreach (var sheet in await GoogleSheetClient.FetchSheetList(apiUrlBox.Text))
            {
                if (sheet.Name.IndexOfAny(Path.GetInvalidFileNameChars()) >= 0)
                {
                    failed = true;
                    Log($"{sheet.Name}: 탭 이름에 파일명으로 쓸 수 없는 문자가 있습니다.");
                    continue;
                }
                var result = SheetJsonConverter.Convert(await GoogleSheetClient.DownloadSheetTsv(spreadsheetUrlBox.Text, sheet.Id));
                if (result.Success)
                {
                    tables.Add((sheet.Name, result.Json));
                    Log($"{sheet.Name}: {result.RowCount}행");
                    continue;
                }
                failed = true;
                Log($"{sheet.Name}: 오류 {result.Errors.Count}개");
                foreach (string error in result.Errors.Take(30)) Log("  " + error);
            }
            if (failed)
            {
                Log("검증에 실패한 탭이 있어 아무 파일도 쓰지 않았습니다.");
                return;
            }

            var serverWrites = tables.Where(t => !SameContent(Path.Combine(serverFolder, t.Name + ".json"), t.Json)).ToList();
            var added = serverWrites.Where(t => !File.Exists(Path.Combine(serverFolder, t.Name + ".json"))).Select(t => t.Name).ToList();
            var changed = serverWrites.Select(t => t.Name).Except(added).ToList();
            Log("");
            Log($"변환 완료: {tables.Count}개 탭 (변경 {changed.Count}, 추가 {added.Count})");
            if (changed.Count > 0) Log("  변경: " + string.Join(", ", changed));
            if (added.Count > 0) Log("  추가: " + string.Join(", ", added));
            Log("");

            string next = versionBox.SelectedIndex switch
            {
                1 => $"{version[0]}.{version[1] + 1}.0",
                2 => $"{version[0] + 1}.0.0",
                _ => serverWrites.Count > 0 ? $"{version[0]}.{version[1]}.{version[2] + 1}" : current
            };
            var clientWrites = clientFolder == null ? new List<(string Name, string Json)>()
                : tables.Where(t => !SameContent(Path.Combine(clientFolder, t.Name + ".json"), t.Json)).ToList();

            string tableList = string.Join(Environment.NewLine, tables.Select(t => t.Name)) + Environment.NewLine;
            string tableListPath = Path.Combine(serverFolder, TableListFileName);
            bool tableListChanged = !SameContent(tableListPath, tableList);

            if (serverWrites.Count == 0 && clientWrites.Count == 0 && !tableListChanged && next == current)
            {
                Log("변경 없음");
                return;
            }
            foreach (var t in serverWrites) Write(Path.Combine(serverFolder, t.Name + ".json"), t.Json);
            if (tableListChanged) Write(tableListPath, tableList);
            foreach (var t in clientWrites) Write(Path.Combine(clientFolder, t.Name + ".json"), t.Json);
            if (next != current) Write(versionFile, next + Environment.NewLine);
            // 수동 버전 선택은 1회만 적용한다
            versionBox.SelectedIndex = 0;

            Log("서버: " + (serverWrites.Count > 0 ? string.Join(", ", serverWrites.Select(t => t.Name)) : "변경 없음"));
            if (tableListChanged) Log($"{TableListFileName}: 탭 목록 갱신");
            if (clientFolder != null)
                Log("클라이언트: " + (clientWrites.Count > 0 ? string.Join(", ", clientWrites.Select(t => t.Name)) : "변경 없음"));
            Log(next != current ? $"버전: {current} → {next}" : $"버전: {current} (유지)");
            Log("커밋 전에 diff를 확인하세요.");
        }

        private void Log(string text) => logBox.AppendText(text + Environment.NewLine);

        private static int[] ParseVersion(string text)
        {
            string[] parts = text.Split('.');
            if (parts.Length == 3 && int.TryParse(parts[0], out int major) && int.TryParse(parts[1], out int minor)
                && int.TryParse(parts[2], out int patch)) return new[] { major, minor, patch };
            throw new FormatException($"version.txt 형식이 올바르지 않습니다: '{text}'");
        }

        // 줄바꿈(CRLF/LF) 차이는 무시한다
        private static bool SameContent(string path, string json) =>
            File.Exists(path) && File.ReadAllText(path).Replace("\r\n", "\n") == json.Replace("\r\n", "\n");

        private static void Write(string path, string text)
        {
            string temporary = path + "." + Guid.NewGuid().ToString("N") + ".tmp";
            try
            {
                File.WriteAllText(temporary, text, new UTF8Encoding(false));
                if (File.Exists(path)) File.Replace(temporary, path, null);
                else File.Move(temporary, path);
                temporary = null;
            }
            finally { if (temporary != null && File.Exists(temporary)) File.Delete(temporary); }
        }
    }
}
