// always_run.tf

resource "null_resource" "always_run" {
  # 매번 업데이트되는 timestamp()를 트리거로 사용
  triggers = {
    run_id = timestamp()
  }
}