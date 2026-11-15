import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/context/AuthContext';
import { Input } from '@/components/common/Input';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';
import { Layers, Lock, Mail, User, ArrowRight } from 'lucide-react';
import { ParsedApiError } from '@/types/api';

const registerSchema = z
  .object({
    username: z
      .string()
      .min(3, 'Le nom d’utilisateur doit comporter au moins 3 caractères')
      .max(50, 'Le nom d’utilisateur ne doit pas dépasser 50 caractères'),
    email: z
      .string()
      .min(1, 'L’adresse email est obligatoire')
      .email('Format d’adresse email invalide')
      .max(100, 'L’adresse email ne doit pas dépasser 100 caractères'),
    password: z
      .string()
      .min(8, 'Le mot de passe doit comporter au minimum 8 caractères')
      .max(72, 'Le mot de passe ne doit pas dépasser 72 caractères'),
    confirmPassword: z.string().min(1, 'Veuillez confirmer votre mot de passe'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Les mots de passe ne correspondent pas',
    path: ['confirmPassword'],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

export const RegisterPage: React.FC = () => {
  const { register: registerAuth } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  });

  const onSubmit = async (data: RegisterFormData) => {
    setServerError(null);
    try {
      await registerAuth({
        username: data.username,
        email: data.email,
        password: data.password,
      });
      navigate('/', { replace: true });
    } catch (err: unknown) {
      const parsed = err as ParsedApiError;
      setServerError(
        parsed.message || 'Impossible de créer le compte. Vérifiez les informations saisies.'
      );
    }
  };

  return (
    <div className="min-h-screen bg-[#09090B] flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Ambient background glow */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-[350px] bg-gradient-to-b from-[#A855F7]/15 via-[#FACC15]/5 to-transparent blur-3xl pointer-events-none" />

      <div className="sm:mx-auto sm:w-full sm:max-w-md relative z-10">
        <div className="flex justify-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#FACC15] text-[#09090B] font-black shadow-[0_0_25px_rgba(250,204,21,0.35)]">
            <Layers className="w-7 h-7" />
          </div>
        </div>
        <h2 className="mt-4 text-center text-2xl font-bold tracking-tight text-[#FAFAFA]">
          Créer un compte
        </h2>
        <p className="mt-1 text-center text-xs text-[#A1A1AA]">
          Rejoignez la plateforme AI Business Platform
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md relative z-10">
        <div className="bg-[#18181B] py-8 px-4 shadow-2xl sm:rounded-2xl sm:px-10 border border-[#27272A] shadow-[0_25px_60px_rgba(0,0,0,0.8)]">
          {serverError && (
            <div className="mb-5">
              <Alert variant="error" title="Erreur d'inscription">
                {serverError}
              </Alert>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Nom d'utilisateur"
              type="text"
              placeholder="ex: jdupont"
              leftIcon={<User className="w-4 h-4" />}
              error={errors.username?.message}
              required
              {...register('username')}
            />

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
              label="Mot de passe (min. 8 caractères)"
              type="password"
              placeholder="••••••••"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.password?.message}
              required
              {...register('password')}
            />

            <Input
              label="Confirmation du mot de passe"
              type="password"
              placeholder="••••••••"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.confirmPassword?.message}
              required
              {...register('confirmPassword')}
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
                Créer mon compte
              </Button>
            </div>
          </form>

          <div className="mt-6 pt-6 border-t border-[#27272A] text-center">
            <p className="text-xs text-[#A1A1AA]">
              Vous avez déjà un compte ?{' '}
              <Link to="/login" className="font-semibold text-[#FACC15] hover:text-[#EAB308] transition-colors">
                Se connecter
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
