{{- define "kafka.ns" -}}
{{- .Values.namespace | default .Release.Namespace -}}
{{- end -}}

