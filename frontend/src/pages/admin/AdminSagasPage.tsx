import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sagasApi } from '@/api/sagasApi';
import { Saga, SagaStatus, SagaDetailsResponse, ReconciliationReport } from '@/types/saga';
import { formatDate, getSagaStatusBadge } from '@/utils/formatters';
import { Table, TableHead, TableBody, TableRow, TableHeaderCell, TableCell } from '@/components/common/Table';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';
import { Modal } from '@/components/common/Modal';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { Input } from '@/components/common/Input';
import {
  RotateCcw,
  CheckCircle2,
  FileSearch,
  Eye,
  GitBranch,
  ShieldAlert,
  Play,
} from 'lucide-react';
import { ParsedApiError } from '@/types/api';

export const AdminSagasPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [selectedSagaId, setSelectedSagaId] = useState<string | null>(null);

  const [sagaToRetry, setSagaToRetry] = useState<Saga | null>(null);
  const [sagaToResolve, setSagaToResolve] = useState<Saga | null>(null);
  const [resolveNote, setResolveNote] = useState<string>('Résolution administrative manuelle');

  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Query sagas list
  const {
    data: sagas = [],
    isLoading,
    isFetching,
    error,
  } = useQuery({
    queryKey: ['sagas', statusFilter],
    queryFn: () =>
      sagasApi.getSagas(statusFilter === 'ALL' ? undefined : (statusFilter as SagaStatus)),
  });

  // Query saga details if one is selected
  const { data: sagaDetails, isLoading: loadingDetails } = useQuery<SagaDetailsResponse>({
    queryKey: ['sagaDetails', selectedSagaId],
    queryFn: () => sagasApi.getSagaDetails(selectedSagaId!),
    enabled: !!selectedSagaId,
  });

  // Query latest reconciliation report
  const { data: reconciliationReport, isLoading: loadingReconciliation } = useQuery<ReconciliationReport>({
    queryKey: ['reconciliationReport'],
    queryFn: () => sagasApi.getLatestReconciliationReport(),
  });

  // Manual retry mutation
  const retryMutation = useMutation({
    mutationFn: (sagaId: string) => sagasApi.retrySaga(sagaId),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['sagas'] });
      setSagaToRetry(null);
      setActionSuccess(`Réessai déclenché pour la Saga ${updated.sagaId}.`);
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Échec du déclenchement du réessai.');
    },
  });

  // Manual resolve mutation
  const resolveMutation = useMutation({
    mutationFn: ({ sagaId, note }: { sagaId: string; note: string }) =>
      sagasApi.resolveSaga(sagaId, note),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['sagas'] });
      setSagaToResolve(null);
      setActionSuccess(`Saga ${updated.sagaId} résolue administrativement.`);
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Échec de la résolution de la Saga.');
    },
  });

  // Run reconciliation audit mutation
  const reconciliationMutation = useMutation({
    mutationFn: (staleMinutes: number) => sagasApi.runReconciliation(staleMinutes),
    onSuccess: (report) => {
      queryClient.setQueryData(['reconciliationReport'], report);
      queryClient.invalidateQueries({ queryKey: ['sagas'] });
      setActionSuccess(
        `Audit de réconciliation terminé : ${report.discrepanciesCount} anomalie(s) détectée(s).`
      );
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Échec du lancement de l’audit de réconciliation.');
    },
  });

  const handleRetryConfirm = async () => {
    if (sagaToRetry) {
      await retryMutation.mutateAsync(sagaToRetry.sagaId);
    }
  };

  const handleResolveConfirm = async () => {
    if (sagaToResolve) {
      await resolveMutation.mutateAsync({
        sagaId: sagaToResolve.sagaId,
        note: resolveNote,
      });
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
              Sagas & Réconciliation de Stock
            </h1>
            <Badge variant="purple">ROLE_ADMIN</Badge>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Supervision de la cohérence distribuée, reprise automatique et audits d'intégrité de stock
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          isLoading={reconciliationMutation.isPending}
          onClick={() => reconciliationMutation.mutate(15)}
          icon={<Play className="w-3.5 h-3.5 text-emerald-600" />}
        >
          Lancer l'audit de réconciliation
        </Button>
      </div>

      {/* Notifications */}
      {actionSuccess && (
        <Alert variant="success" onDismiss={() => setActionSuccess(null)}>
          {actionSuccess}
        </Alert>
      )}

      {actionError && (
        <Alert variant="error" onDismiss={() => setActionError(null)}>
          {actionError}
        </Alert>
      )}

      {error && (
        <Alert variant="error" title="Erreur lors de la récupération des Sagas">
          {(error as unknown as ParsedApiError).message || 'Service inaccessible ou privilèges insuffisants.'}
        </Alert>
      )}

      {/* Latest Reconciliation Audit Overview Banner */}
      <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-bold text-slate-700">
            <FileSearch className="w-4 h-4 text-emerald-600" />
            <span>Dernier rapport d'audit de réconciliation (read-only)</span>
          </div>
          {loadingReconciliation ? (
            <p className="text-xs text-slate-400 mt-1">Chargement du rapport...</p>
          ) : reconciliationReport ? (
            <p className="text-xs text-slate-500 mt-1">
              Exécuté le <strong>{formatDate(reconciliationReport.executedAt)}</strong> • Sagas
              vérifiées : <strong>{reconciliationReport.sagasChecked}</strong> • Ventes auditées :{' '}
              <strong>{reconciliationReport.salesChecked}</strong> • Anomalies constatées :{' '}
              <strong
                className={
                  reconciliationReport.discrepanciesCount > 0
                    ? 'text-rose-600'
                    : 'text-emerald-600'
                }
              >
                {reconciliationReport.discrepanciesCount}
              </strong>
            </p>
          ) : (
            <p className="text-xs text-slate-400 mt-1">Aucun rapport d'audit récent disponible.</p>
          )}
        </div>

        {reconciliationReport && reconciliationReport.discrepanciesCount > 0 && (
          <Badge variant="danger" size="md">
            Attention requise ({reconciliationReport.discrepanciesCount})
          </Badge>
        )}
      </div>

      {/* Filter Tabs */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3 rounded-xl border border-slate-200 shadow-xs">
        <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto">
          {[
            'ALL',
            'COMPENSATION_PENDING',
            'FAILED_REQUIRES_ATTENTION',
            'COMPENSATED',
            'COMPLETED',
          ].map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                statusFilter === st
                  ? 'bg-slate-900 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {st === 'ALL' ? 'Toutes les Sagas' : st}
            </button>
          ))}
        </div>

        <div className="text-xs text-slate-500">
          {isFetching && <span className="text-emerald-600 font-medium mr-2">Actualisation...</span>}
          <span>{sagas.length} Saga(s) affichée(s)</span>
        </div>
      </div>

      {/* Sagas Table */}
      {isLoading ? (
        <LoadingSpinner label="Chargement des Sagas distribuées..." />
      ) : sagas.length === 0 ? (
        <div className="bg-white p-10 text-center rounded-xl border border-dashed border-slate-300 text-slate-500">
          <GitBranch className="w-8 h-8 mx-auto text-slate-400 mb-2" />
          <p className="text-sm font-semibold text-slate-700">Aucune Saga trouvée</p>
          <p className="text-xs text-slate-400 mt-1">
            Aucune orchestration ne correspond au filtre actuel.
          </p>
        </div>
      ) : (
        <Table>
          <TableHead>
            <tr>
              <TableHeaderCell>ID Saga</TableHeaderCell>
              <TableHeaderCell>Type & Réf. Vente</TableHeaderCell>
              <TableHeaderCell>Statut</TableHeaderCell>
              <TableHeaderCell>Tentatives</TableHeaderCell>
              <TableHeaderCell>Dernière erreur</TableHeaderCell>
              <TableHeaderCell>Créée le</TableHeaderCell>
              <TableHeaderCell className="text-right">Actions</TableHeaderCell>
            </tr>
          </TableHead>
          <TableBody>
            {sagas.map((s) => {
              const badge = getSagaStatusBadge(s.status);
              const canRetry =
                s.status === 'FAILED_REQUIRES_ATTENTION' || s.status === 'COMPENSATION_PENDING';
              const canResolve =
                s.status === 'FAILED_REQUIRES_ATTENTION' || s.status === 'COMPENSATION_PENDING';

              return (
                <TableRow key={s.id}>
                  <TableCell className="font-mono text-xs font-bold text-slate-900">
                    <button
                      type="button"
                      onClick={() => setSelectedSagaId(s.sagaId)}
                      className="text-emerald-600 hover:underline flex items-center gap-1 text-left"
                    >
                      <Eye className="w-3 h-3 shrink-0" />
                      <span>{s.sagaId}</span>
                    </button>
                  </TableCell>
                  <TableCell>
                    <div className="font-semibold text-xs text-slate-800">{s.sagaType}</div>
                    <div className="font-mono text-[11px] text-slate-400 mt-0.5">
                      Réf: {s.saleReference}
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant={badge.variant} size="sm">
                      {badge.label}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-xs font-semibold text-slate-700">
                    {s.attemptCount} / {s.maxAttempts}
                  </TableCell>
                  <TableCell className="text-xs text-rose-600 max-w-xs truncate" title={s.lastError || ''}>
                    {s.lastError || <span className="text-slate-400 italic">Aucune</span>}
                  </TableCell>
                  <TableCell className="text-xs text-slate-400">
                    {formatDate(s.createdAt)}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      {canRetry && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setSagaToRetry(s)}
                          title="Déclencher un réessai de compensation"
                          aria-label={`Réessayer ${s.sagaId}`}
                        >
                          <RotateCcw className="w-3.5 h-3.5 text-amber-600" />
                        </Button>
                      )}
                      {canResolve && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setSagaToResolve(s);
                            setResolveNote('Résolution administrative manuelle');
                          }}
                          title="Résoudre manuellement cette saga"
                          aria-label={`Résoudre ${s.sagaId}`}
                        >
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}

      {/* Saga Event Log Details Modal */}
      {selectedSagaId && (
        <Modal
          isOpen={!!selectedSagaId}
          onClose={() => setSelectedSagaId(null)}
          title={`Journal de la Saga : ${selectedSagaId}`}
          description="Historique des transitions d'état et événements transactionnels"
          maxWidth="lg"
        >
          {loadingDetails ? (
            <LoadingSpinner label="Chargement des événements..." />
          ) : !sagaDetails ? (
            <Alert variant="error">Impossible de récupérer les détails de la Saga.</Alert>
          ) : (
            <div className="space-y-4">
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs grid grid-cols-2 gap-2">
                <div>
                  Statut : <strong>{sagaDetails.saga.status}</strong>
                </div>
                <div>
                  Tentatives :{' '}
                  <strong>
                    {sagaDetails.saga.attemptCount} / {sagaDetails.saga.maxAttempts}
                  </strong>
                </div>
                <div>
                  Réservation : <code>{sagaDetails.saga.reservationId}</code>
                </div>
                <div>
                  Créée : {formatDate(sagaDetails.saga.createdAt)}
                </div>
                {sagaDetails.saga.lastError && (
                  <div className="col-span-2 text-rose-600">
                    Erreur : {sagaDetails.saga.lastError}
                  </div>
                )}
              </div>

              <div>
                <h4 className="text-xs font-bold uppercase text-slate-600 mb-2">
                  Événements enregistrés ({sagaDetails.events?.length || 0})
                </h4>
                <div className="space-y-2 max-h-60 overflow-y-auto">
                  {sagaDetails.events?.map((ev) => (
                    <div
                      key={ev.id}
                      className="p-2.5 rounded-lg border border-slate-200 bg-white text-xs hover:bg-slate-50"
                    >
                      <div className="flex items-center justify-between font-semibold text-slate-800">
                        <span>{ev.eventType}</span>
                        <span className="text-[11px] text-slate-400 font-normal">
                          {formatDate(ev.createdAt)}
                        </span>
                      </div>
                      <div className="text-slate-600 mt-1">{ev.message}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </Modal>
      )}

      {/* Retry Confirmation Dialog */}
      {sagaToRetry && (
        <ConfirmDialog
          isOpen={!!sagaToRetry}
          onClose={() => setSagaToRetry(null)}
          onConfirm={handleRetryConfirm}
          title="Réessayer la compensation Saga"
          message={`Voulez-vous réinitialiser le statut de la Saga ${sagaToRetry.sagaId} à COMPENSATION_PENDING et réveiller immédiatement le SagaRecoveryWorker ?`}
          confirmLabel="Lancer la compensation"
          variant="primary"
          isLoading={retryMutation.isPending}
        />
      )}

      {/* Resolve Saga Modal with Note */}
      {sagaToResolve && (
        <Modal
          isOpen={!!sagaToResolve}
          onClose={() => setSagaToResolve(null)}
          title="Résolution administrative manuelle"
          description={`Cette action force le statut de la Saga ${sagaToResolve.sagaId} à COMPENSATED.`}
          maxWidth="md"
        >
          <div className="space-y-4">
            <div className="flex items-start gap-2 text-xs text-amber-800 bg-amber-50 p-3 rounded-lg border border-amber-200">
              <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <span>
                À n'utiliser qu'après avoir vérifié que le stock a été régularisé manuellement dans le
                service catalogue.
              </span>
            </div>

            <Input
              label="Note / Justification opérationnelle"
              value={resolveNote}
              onChange={(e) => setResolveNote(e.target.value)}
              placeholder="ex: Stock ajusté manuellement en base"
            />

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
              <Button variant="outline" onClick={() => setSagaToResolve(null)}>
                Annuler
              </Button>
              <Button
                variant="primary"
                onClick={handleResolveConfirm}
                isLoading={resolveMutation.isPending}
              >
                Confirmer la résolution
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
