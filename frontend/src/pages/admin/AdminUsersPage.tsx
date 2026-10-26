import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { authApi } from '@/api/authApi';
import { UserSummary, UserRole } from '@/types/auth';
import { useAuth } from '@/context/AuthContext';
import { formatDate } from '@/utils/formatters';
import { Table, TableHead, TableBody, TableRow, TableHeaderCell, TableCell } from '@/components/common/Table';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { Shield, User, ShieldCheck } from 'lucide-react';
import { ParsedApiError } from '@/types/api';

export const AdminUsersPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { user: currentLoggedUser } = useAuth();

  const [roleChangeTarget, setRoleChangeTarget] = useState<{
    user: UserSummary;
    targetRole: UserRole;
  } | null>(null);

  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const {
    data: users = [],
    isLoading,
    isFetching,
    error,
  } = useQuery({
    queryKey: ['adminUsers'],
    queryFn: () => authApi.getAllUsers(),
  });

  const updateRoleMutation = useMutation({
    mutationFn: ({ id, role }: { id: number; role: UserRole }) =>
      authApi.updateUserRole(id, { role }),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['adminUsers'] });
      setRoleChangeTarget(null);
      setActionSuccess(`Rôle de l'utilisateur "${updated.username}" mis à jour vers ${updated.role}.`);
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Impossible de modifier le rôle.');
    },
  });

  const handleConfirmRoleChange = async () => {
    if (roleChangeTarget) {
      await updateRoleMutation.mutateAsync({
        id: roleChangeTarget.user.id,
        role: roleChangeTarget.targetRole,
      });
    }
  };

  return (
    <div className="space-y-6">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
              Gestion des Utilisateurs & Rôles (RBAC)
            </h1>
            <Badge variant="purple">ROLE_ADMIN</Badge>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Attribution des privilèges <code>ROLE_USER</code> et <code>ROLE_ADMIN</code> (microservice <code>auth-service</code>)
          </p>
        </div>
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
        <Alert variant="error" title="Erreur lors de la récupération des utilisateurs">
          {(error as unknown as ParsedApiError).message || 'Accès refusé ou service inaccessible.'}
        </Alert>
      )}

      {/* Overview Card */}
      <div className="p-4 bg-white rounded-xl border border-slate-200 shadow-xs flex items-center justify-between text-xs text-slate-600">
        <div className="flex items-center gap-2">
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
          <span>Matrice RBAC vérifiée : Seuls les comptes <strong>ROLE_ADMIN</strong> peuvent administrer les rôles et le catalogue.</span>
        </div>
        <div className="font-semibold">
          Total : {users.length} utilisateur(s)
          {isFetching && <span className="text-emerald-600 ml-2 font-normal">(actualisation...)</span>}
        </div>
      </div>

      {/* Main Table */}
      {isLoading ? (
        <LoadingSpinner label="Chargement des comptes utilisateurs..." />
      ) : (
        <Table>
          <TableHead>
            <tr>
              <TableHeaderCell>ID</TableHeaderCell>
              <TableHeaderCell>Utilisateur</TableHeaderCell>
              <TableHeaderCell>Adresse Email</TableHeaderCell>
              <TableHeaderCell>Rôle Actuel</TableHeaderCell>
              <TableHeaderCell>Date d'inscription</TableHeaderCell>
              <TableHeaderCell className="text-right">Actions</TableHeaderCell>
            </tr>
          </TableHead>
          <TableBody>
            {users.map((u) => {
              const isAdmin = u.role === 'ADMIN';
              const isCurrentUser = String(u.id) === currentLoggedUser?.userId;

              return (
                <TableRow key={u.id}>
                  <TableCell className="font-mono text-xs text-slate-400">#{u.id}</TableCell>
                  <TableCell>
                    <div className="font-semibold text-slate-900 flex items-center gap-1.5">
                      {isAdmin ? (
                        <Shield className="w-3.5 h-3.5 text-purple-600 shrink-0" />
                      ) : (
                        <User className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      )}
                      <span>{u.username}</span>
                      {isCurrentUser && (
                        <span className="text-[10px] bg-slate-100 text-slate-600 px-1.5 py-0.2 rounded font-medium">
                          Vous
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-xs text-slate-600">{u.email}</TableCell>
                  <TableCell>
                    <Badge variant={isAdmin ? 'purple' : 'info'} size="sm">
                      {u.role}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-xs text-slate-400">
                    {formatDate(u.createdAt)}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      {isAdmin ? (
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={isCurrentUser}
                          onClick={() => setRoleChangeTarget({ user: u, targetRole: 'USER' })}
                          title={isCurrentUser ? 'Vous ne pouvez pas révoquer vos propres droits' : 'Rétrograder à USER'}
                          aria-label={`Rétrograder ${u.username} au rôle USER`}
                        >
                          Rétrograder en USER
                        </Button>
                      ) : (
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => setRoleChangeTarget({ user: u, targetRole: 'ADMIN' })}
                          title="Promouvoir au rôle Administrateur"
                          aria-label={`Promouvoir ${u.username} au rôle ADMIN`}
                        >
                          Promouvoir en ADMIN
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

      {/* Confirm Role Change Dialog */}
      {roleChangeTarget && (
        <ConfirmDialog
          isOpen={!!roleChangeTarget}
          onClose={() => setRoleChangeTarget(null)}
          onConfirm={handleConfirmRoleChange}
          title="Modifier les privilèges utilisateur"
          message={`Êtes-vous certain de vouloir modifier le rôle de "${roleChangeTarget.user.username}" de ${roleChangeTarget.user.role} vers ${roleChangeTarget.targetRole} ?`}
          confirmLabel={`Passer en ${roleChangeTarget.targetRole}`}
          variant={roleChangeTarget.targetRole === 'ADMIN' ? 'primary' : 'danger'}
          isLoading={updateRoleMutation.isPending}
        />
      )}
    </div>
  );
};
