import React, { useState } from 'react';
import { useNavigate, useLocation, Link, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/context/AuthContext';
import { Input } from '@/components/common/Input';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';
import { Layers, Lock, Mail, ArrowRight, ShieldAlert } from 'lucide-react';
import { ParsedApiError } from '@/types/api';

const loginSchema = z.object({
  email: z.string().min(1, 'L’adresse email est obligatoire').email('Format d’adresse email invalide'),
  password: z.string().min(1, 'Le mot de passe est obligatoire'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const [serverError, setServerError] = useState<string | null>(null);

  const isExpired = searchParams.get('expired') === 'true';
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null);
    try {
      await login(data);
      navigate(from, { replace: true });
    } catch (err: unknown) {
      const parsed = err as ParsedApiError;
      setServerError(parsed.message || 'Identifiants invalides ou service injoignable.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-lg">
            <Layers className="w-7 h-7" />
          </div>
        </div>
        <h2 className="mt-4 text-center text-2xl font-bold tracking-tight text-slate-900">
          AI Business Platform
        </h2>
        <p className="mt-1 text-center text-xs text-slate-500">
          Plateforme de gestion commerciale microservices
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow-xl shadow-slate-200/60 sm:rounded-2xl sm:px-10 border border-slate-200/80">
          <h3 className="text-lg font-bold text-slate-900 mb-6">Connexion à votre espace</h3>

          {isExpired && (
            <div className="mb-5">
              <Alert variant="warning" title="Session expirée">
                Votre session a expiré pour des raisons de sécurité. Veuillez vous reconnecter.
              </Alert>
            </div>
          )}

          {serverError && (
            <div className="mb-5">
              <Alert variant="error" title="Échec de connexion">
                {serverError}
              </Alert>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Adresse email"
              type="email"
              placeholder="ex: jean.dupont@example.com"
              leftIcon={<Mail className="w-4 h-4" />}
              error={errors.email?.message}
              required
              {...register('email')}
            />

            <Input
              label="Mot de passe"
              type="password"
              placeholder="••••••••"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.password?.message}
              required
              {...register('password')}
            />

            <div className="pt-2">
              <Button
                type="submit"
                variant="primary"
                size="md"
                className="w-full"
                isLoading={isSubmitting}
                icon={<ArrowRight className="w-4 h-4" />}
              >
                Se connecter
              </Button>
            </div>
          </form>

          <div className="mt-6 pt-6 border-t border-slate-100 text-center">
            <p className="text-xs text-slate-500">
              Pas encore de compte ?{' '}
              <Link to="/register" className="font-semibold text-emerald-600 hover:text-emerald-700">
                Créer un compte
              </Link>
            </p>
          </div>

          {/* Quick connection tip */}
          <div className="mt-6 p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs text-slate-600 space-y-1">
            <div className="font-semibold flex items-center gap-1.5 text-slate-700">
              <ShieldAlert className="w-3.5 h-3.5 text-amber-600" />
              <span>Authentification JWT & Passerelle</span>
            </div>
            <p className="text-[11px] text-slate-500">
              Vous pouvez vous connecter avec votre compte utilisateur ou créer un nouveau compte via
              le lien ci-dessus.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
